package com.sd20201.datn.infrastructure.email;

import com.sd20201.datn.entity.Shift;
import com.sd20201.datn.entity.ShiftHandover;
import com.sd20201.datn.entity.Account;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ShiftReportService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendReport(String toEmail, ShiftHandover handover, Shift shiftTemplate) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String displayShiftName = (shiftTemplate != null) ? shiftTemplate.getName() : handover.getName();

            helper.setFrom(fromEmail, "Hệ thống MyLaptop");
            helper.setTo(toEmail);
            helper.setSubject("📑 Báo cáo Kết ca: " + displayShiftName + " - " + formatDate(LocalDateTime.now()));

            // --- 1. THÔNG TIN NHÂN VIÊN ---
            String staffName = "N/A";
            String staffCode = "";
            if (handover.getAccount() != null) {
                Account acc = handover.getAccount();
                if (acc.getStaff() != null) {
                    staffName = acc.getStaff().getName();
                    staffCode = acc.getStaff().getCode();
                } else {
                    staffName = acc.getUsername();
                }
            }

            // --- 2. TÀI CHÍNH (ĐÃ FIX LOGIC) ---
            double initial = getSafeDouble(handover.getInitialCash());

            // Lấy tổng tiền mặt lý thuyết (Đã có sẵn từ BE, không được cộng thêm initial nữa)
            double expectedCash = getSafeDouble(handover.getTotalCashAmount());

            // Tính ngược lại Doanh thu tiền mặt bán được
            double cashRevenue = Math.max(0, expectedCash - initial);

            // Lấy Doanh thu chuyển khoản
            double transferRevenue = getSafeDouble(handover.getTotalTransferAmount());

            double realCash = getSafeDouble(handover.getRealCashAmount());

            // Chênh lệch = Thực tế đếm - Lý thuyết hệ thống
            double diff = realCash - expectedCash;

            Locale localeVN = new Locale("vi", "VN");
            NumberFormat currencyVN = NumberFormat.getCurrencyInstance(localeVN);
            String diffString = (diff >= 0 ? "+" : "") + currencyVN.format(diff);
            String colorDiff = diff >= 0 ? "#16a34a" : "#dc2626";

            // --- 3. GIỜ GIẤC ---
            String timeStatus = "Ca tự do";
            String scheduledRange = "Không quy định";
            if (shiftTemplate != null && shiftTemplate.getStartTime() != null) {
                String startT = shiftTemplate.getStartTime();
                String endT = shiftTemplate.getEndTime() != null ? shiftTemplate.getEndTime() : "--:--";
                scheduledRange = startT + " - " + endT;
                timeStatus = analyzeTime(startT, handover.getStartTime());
            }

            // --- 4. GHI CHÚ ---
            String rawNote = handover.getNote();
            String startNote = "Không có";
            String endNote = "Không có";
            if (rawNote != null && !rawNote.isEmpty()) {
                String[] parts = rawNote.split(" \\| ");
                if (parts.length >= 1) startNote = parts[0].trim();
                if (parts.length >= 2) endNote = parts[1].trim();
            }

            // --- 5. HTML ---
            String htmlContent = String.format("""
                <html>
                <body style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; background-color: #f4f4f5; padding: 20px; margin: 0;">
                    <div style="max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">
                        <div style="background: linear-gradient(135deg, #10b981 0%%, #059669 100%%); padding: 30px 20px; text-align: center; color: white;">
                            <h1 style="margin: 0; font-size: 24px; font-weight: 700;">BÁO CÁO KẾT CA</h1>
                            <p style="margin: 5px 0 0; opacity: 0.9; font-size: 14px;">%s</p>
                        </div>
                        <div style="padding: 24px;">
                            <table style="width: 100%%; border-bottom: 1px solid #e5e7eb; margin-bottom: 24px; padding-bottom: 15px;">
                                <tr>
                                    <td style="text-align: left;">
                                        <p style="margin: 0; color: #6b7280; font-size: 11px; text-transform: uppercase; font-weight: 700;">NHÂN VIÊN TRỰC</p>
                                        <p style="margin: 4px 0 0; font-size: 16px; font-weight: 700; color: #1f2937;">%s <span style="font-weight: 400; color: #6b7280; font-size: 14px;">(%s)</span></p>
                                    </td>
                                    <td style="text-align: right;">
                                        <p style="margin: 0; color: #6b7280; font-size: 11px; text-transform: uppercase; font-weight: 700;">CA LÀM VIỆC</p>
                                        <p style="margin: 4px 0 0; font-size: 16px; font-weight: 700; color: #10b981;">%s</p>
                                    </td>
                                </tr>
                            </table>
                            <div style="background-color: #f9fafb; border-radius: 8px; padding: 16px; margin-bottom: 24px;">
                                <h3 style="margin: 0 0 12px 0; font-size: 13px; color: #4b5563; text-transform: uppercase;">⏰ Thời gian làm việc</h3>
                                <table style="width: 100%%; font-size: 14px;">
                                    <tr><td style="color: #6b7280; padding-bottom: 8px;">Quy định:</td><td style="text-align: right; font-weight: 600;">%s</td></tr>
                                    <tr><td style="color: #6b7280; padding-bottom: 8px;">Check-in thực tế:</td><td style="text-align: right; font-weight: 600;">%s</td></tr>
                                    <tr><td style="color: #6b7280;">Trạng thái:</td><td style="text-align: right;">%s</td></tr>
                                    <tr><td style="color: #6b7280; padding-top: 8px; border-top: 1px dashed #e5e7eb;">Thời gian kết ca:</td><td style="text-align: right; font-weight: 600; padding-top: 8px; border-top: 1px dashed #e5e7eb;">%s</td></tr>
                                </table>
                            </div>
                            <div style="border: 2px solid #e5e7eb; border-radius: 12px; padding: 20px; margin-bottom: 24px;">
                                <div style="display: flex; justify-content: space-between; margin-bottom: 15px;">
                                    <h3 style="margin: 0; font-size: 16px; color: #111827;">💵 Tổng kết tài chính</h3>
                                    <span style="background: #eff6ff; color: #2563eb; padding: 4px 10px; border-radius: 20px; font-size: 12px; font-weight: 600;">%d đơn hàng</span>
                                </div>
                                <table style="width: 100%%; font-size: 15px;">
                                    <tr><td style="padding: 6px 0;">Tiền đầu ca:</td><td style="text-align: right; font-family: monospace;">%s</td></tr>
                                    <tr><td style="padding: 6px 0;">Doanh thu Tiền mặt:</td><td style="text-align: right; font-family: monospace; color: #059669;">+%s</td></tr>
                                    <tr><td style="padding: 6px 0;">Doanh thu CK / Thẻ:</td><td style="text-align: right; font-family: monospace; color: #2563eb;">+%s</td></tr>
                                    <tr style="border-top: 1px solid #e5e7eb;"><td style="padding-top: 12px; font-weight: 700;">Lý thuyết tại két:</td><td style="padding-top: 12px; text-align: right; font-weight: 700; font-family: monospace;">%s</td></tr>
                                    <tr><td style="font-weight: 700;">Thực tế thu (Tiền mặt):</td><td style="text-align: right; font-weight: 700; font-family: monospace; color: #000;">%s</td></tr>
                                    <tr><td style="padding-top: 6px; font-size: 14px; font-style: italic;">Chênh lệch két:</td><td style="padding-top: 6px; text-align: right; font-weight: 700; color: %s;">%s</td></tr>
                                </table>
                            </div>
                            <div style="background-color: #fffbeb; border: 1px solid #fcd34d; border-radius: 8px; overflow: hidden;">
                                <div style="background-color: #fef3c7; padding: 10px 15px; border-bottom: 1px solid #fcd34d;">
                                    <p style="margin: 0; font-weight: 700; color: #b45309; font-size: 13px; text-transform: uppercase;">📝 Ghi chú ca làm việc</p>
                                </div>
                                <div style="padding: 15px; font-size: 14px; color: #4b5563;">
                                    <p style="margin: 0 0 4px 0; font-weight: 700; color: #92400e; font-size: 11px;">▶ ĐẦU CA:</p>
                                    <p style="margin: 0;">%s</p>
                                    <hr style="border: 0; border-top: 1px dashed #e5e7eb; margin: 10px 0;" />
                                    <p style="margin: 0 0 4px 0; font-weight: 700; color: #047857; font-size: 11px;">⏹ CUỐI CA:</p>
                                    <p style="margin: 0;">%s</p>
                                </div>
                            </div>
                        </div>
                        <div style="background-color: #f3f4f6; padding: 15px; text-align: center; border-top: 1px solid #e5e7eb;">
                            <p style="margin: 0; font-size: 12px; color: #9ca3af;">
                                Email tự động từ hệ thống MyLaptop • ID: %s
                            </p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                    formatDate(LocalDateTime.now()),
                    staffName, staffCode,
                    displayShiftName,
                    scheduledRange,
                    formatTime(handover.getStartTime()),
                    timeStatus,
                    formatTime(handover.getEndTime()),
                    handover.getTotalBills() != null ? handover.getTotalBills() : 0,
                    currencyVN.format(initial),
                    currencyVN.format(cashRevenue),
                    currencyVN.format(transferRevenue), // 👈 ĐÃ BỔ SUNG DÒNG NÀY
                    currencyVN.format(expectedCash),
                    currencyVN.format(realCash),
                    colorDiff, diffString,
                    startNote,
                    endNote,
                    handover.getId()
            );

            helper.setText(htmlContent, true);
            javaMailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double getSafeDouble(BigDecimal val) {
        return val != null ? val.doubleValue() : 0.0;
    }

    private String analyzeTime(String scheduledTimeStr, LocalDateTime actualCheckIn) {
        try {
            if (scheduledTimeStr == null || actualCheckIn == null) return "---";
            LocalTime scheduled = LocalTime.parse(scheduledTimeStr);
            LocalTime actual = actualCheckIn.toLocalTime();
            long diffMinutes = Duration.between(scheduled, actual).toMinutes();
            if (diffMinutes > 15) return "<span style='color: #dc2626; font-weight: 700;'>ĐI MUỘN " + diffMinutes + " phút</span>";
            if (diffMinutes < -5) return "<span style='color: #16a34a; font-weight: 700;'>ĐẾN SỚM " + Math.abs(diffMinutes) + " phút</span>";
            return "<span style='color: #16a34a; font-weight: 700;'>ĐÚNG GIỜ</span>";
        } catch (Exception e) { return "---"; }
    }

    private String formatTime(LocalDateTime time) {
        return time != null ? time.format(DateTimeFormatter.ofPattern("HH:mm")) : "--:--";
    }

    private String formatDate(LocalDateTime time) {
        return time != null ? time.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
    }
}