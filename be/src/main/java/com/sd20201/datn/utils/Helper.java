package com.sd20201.datn.utils;

import com.sd20201.datn.core.admin.voucher.voucher.model.request.AdVoucherCreateUpdateRequest;
import com.sd20201.datn.core.common.base.PageableRequest;
import com.sd20201.datn.core.common.base.ResponseObject;
import com.sd20201.datn.entity.Customer;
import com.sd20201.datn.entity.Discount;
import com.sd20201.datn.entity.Voucher;
import com.sd20201.datn.infrastructure.constant.EntityStatus;
import com.sd20201.datn.infrastructure.constant.PaginationConstant;
import com.sd20201.datn.infrastructure.constant.TypeVoucher;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class Helper {

    // --- Utils methods cơ bản ---
    public static String appendWildcard(String url) {
        return url + "/**";
    }

    public static Pageable createPageable(PageableRequest request, String defaultSortBy) {
        return PageRequest.of(request.getPage() - 1, request.getSize() == 0 ? PaginationConstant.DEFAULT_SIZE : request.getSize(), Sort.by((Sort.Direction.fromString(request.getOrderBy()) == Sort.Direction.DESC || request.getOrderBy() == null) ? Sort.Direction.DESC : Sort.Direction.ASC, (request.getSortBy() == null || request.getSortBy().isEmpty()) ? defaultSortBy : request.getSortBy()));
    }

    public static Pageable createPageable(PageableRequest request, String defaultSortBy, String defaultOrderBy) {
        int page = request.getPage() - 1;
        int size = request.getSize() == 0 ? PaginationConstant.DEFAULT_SIZE : request.getSize();
        Sort.Direction direction = request.getOrderBy() == null || request.getOrderBy().isEmpty() ? Sort.Direction.fromString(defaultOrderBy) : Sort.Direction.fromString(request.getOrderBy());
        String sortBy = request.getSortBy();
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = defaultSortBy;
        }
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    public static Pageable createPageable(PageableRequest request) {
        return createPageable(request, "createdDate", "DESC");
    }

    public static ResponseEntity<?> createResponseEntity(ResponseObject<?> responseObject) {
        return new ResponseEntity<>(responseObject, responseObject.getStatus());
    }

    public static boolean isValidPhoneNumber(String phoneNumber) {
        String regex = "^0[0-9]{9,10}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(phoneNumber);
        return !matcher.matches();
    }

    public static String replaceManySpaceToOneSpace(String name) {
        return name.replaceAll("\\s+", " ");
    }

    public static String replaceSpaceToEmpty(String name) {
        return name.replaceAll("\\s+", "");
    }

    private static final Map<Character, Character> SPECIAL_CHAR_MAP = new HashMap<>();

    static {
        SPECIAL_CHAR_MAP.put('đ', 'd');
        SPECIAL_CHAR_MAP.put('Đ', 'D');
    }

    public static String generateCodeFromName(String name) {
        String upperCaseString = name.toUpperCase();
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : upperCaseString.toCharArray()) {
            if (SPECIAL_CHAR_MAP.containsKey(c)) {
                stringBuilder.append(SPECIAL_CHAR_MAP.get(c));
            } else {
                stringBuilder.append(c);
            }
        }
        String replacedString = stringBuilder.toString();
        String normalizedString = Normalizer.normalize(replacedString, Normalizer.Form.NFD);
        String withoutAccentString = normalizedString.replaceAll("\\p{M}", "");
        return withoutAccentString.replaceAll("\\s+", "_");
    }

    public static String generateCodeProductDetail() {
        return "PD" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public static String generateCodeVoucher() {
        return "VC" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public static String generateCodeVoucherDetail() {
        return "VD" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public static String buildEmailContent(Discount discount) {
        return "";
    }

    // --- [MỚI] LOGIC VALIDATE DỮ LIỆU ĐẦU VÀO ---
    public static void validateVoucherInput(AdVoucherCreateUpdateRequest request) throws BadRequestException {
        // 1. Validate Ngày tháng
        Long now = DateTimeUtil.getCurrentTimeMillisecondsStamp();
        if (request.getStartDate() == null || request.getEndDate() == null)
            throw new BadRequestException("Thời gian không được để trống!");
        if (request.getStartDate() >= request.getEndDate())
            throw new BadRequestException("Ngày bắt đầu phải nhỏ hơn ngày kết thúc!");
        if (request.getStartDate() < now) {
            throw new BadRequestException("Ngày bắt đầu không được ở trong quá khứ");
        }
        if (request.getEndDate() < now) {
            throw new BadRequestException("Ngày kết thúc không được ở trong quá khứ");
        }

        // 2. Validate Độ dài tên (Dưới 50 ký tự)
        if (request.getName() != null && request.getName().trim().length() > 50) {
            throw new BadRequestException("Tên voucher quá dài! Vui lòng đặt dưới 50 ký tự.");
        }

        // 3. Validate Giá trị tiền (Dưới 100 triệu và 1 tỷ)
        BigDecimal limit100Mil = new BigDecimal("100000000"); // 100 triệu
        BigDecimal limit1Bil = new BigDecimal("1000000000");  // 1 tỷ

        if (request.getMaxValue() != null && request.getMaxValue().compareTo(limit100Mil) >= 0) {
            throw new BadRequestException("Giá giảm tối đa phải nhỏ hơn 100 triệu VNĐ!");
        }

        if (request.getConditions() != null && request.getConditions().compareTo(limit1Bil) >= 0) {
            throw new BadRequestException("Điều kiện đơn hàng phải nhỏ hơn 1 tỷ VNĐ!");
        }
    }

    public static void mapRequestToVoucher(AdVoucherCreateUpdateRequest request, Voucher voucher) {
        voucher.setName(request.getName().trim());
        voucher.setTargetType(request.getTargetType());
        voucher.setTypeVoucher(request.getTypeVoucher());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMaxValue(request.getMaxValue());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setConditions(request.getConditions());
        voucher.setNote(request.getNote());
        voucher.setStatus(EntityStatus.ACTIVE);
    }

    public static boolean isVoucherContentChanged(Voucher oldVoucher, AdVoucherCreateUpdateRequest newRequest) {
        if (!oldVoucher.getName().equals(newRequest.getName())) return true;
        if (oldVoucher.getTypeVoucher() != newRequest.getTypeVoucher()) return true;
        if (!Objects.equals(oldVoucher.getDiscountValue(), newRequest.getDiscountValue())) return true;
        if (!Objects.equals(oldVoucher.getMaxValue(), newRequest.getMaxValue())) return true;
        if (!Objects.equals(oldVoucher.getConditions(), newRequest.getConditions())) return true;
        if (!Objects.equals(oldVoucher.getStartDate(), newRequest.getStartDate())) return true;
        if (!Objects.equals(oldVoucher.getEndDate(), newRequest.getEndDate())) return true;
        return false;
    }

    // =================================================================================
    //  PHẦN GIAO DIỆN EMAIL - FINAL ARTISTIC TICKET
    // =================================================================================

    private static String getTicketHtmlTemplate(String headerColor, String customerName, String messageBody,
                                                String logoUrl, String statusBadge, String voucherName, String discountDisplay,
                                                String maxReduceStr, String voucherCode, String infoTableRows, String btnText, String btnColor) {
        // Link Logo dự phòng
        if (logoUrl == null || logoUrl.isEmpty()) {
            logoUrl = "https://caodang.fpt.edu.vn/wp-content/uploads/2018/01/logo-fpt-polytechnic.png";
        }

        return """
                <!DOCTYPE html>
                <html>
                <head>
                <style>
                    /* Import Font Nghệ thuật từ Google Fonts */
                    @import url('https://fonts.googleapis.com/css2?family=Dancing+Script:wght@700&family=Playfair+Display:wght@700&display=swap');
                
                    body { font-family: 'Arial', sans-serif; line-height: 1.6; color: #333; background-color: #eeeeee; margin: 0; padding: 0; }
                    .container { max-width: 650px; margin: 30px auto; background: #eeeeee; }
                
                    .header { background-color: {{headerColor}}; color: #ffffff; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }
                    .header h1 { margin: 0; font-size: 24px; font-weight: 800; text-transform: uppercase; letter-spacing: 1px; }
                
                    .voucher-wrapper { margin: 25px 0; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.15); font-family: 'Arial', sans-serif; position: relative; background: transparent; }
                    .voucher-table { width: 100%; border-collapse: collapse; background-color: transparent; }
                
                    .left-side { background-color: #1a4d2e; width: 28%; text-align: center; vertical-align: middle; padding: 20px 10px; border-right: 2px dashed #f8f9fa; position: relative; }
                    .vertical-text { color: #fff; font-size: 26px; font-weight: bold; text-transform: uppercase; writing-mode: vertical-rl; text-orientation: mixed; letter-spacing: 6px; display: inline-block; transform: rotate(-180deg); }
                
                    .right-side { background-color: #fffdf5; width: 72%; padding: 25px 30px; vertical-align: middle; position: relative; }
                
                    .logo-img { float: right; width: 80px; height: 80px; object-fit: contain; margin-left: 10px; }
                
                    /* --- FONT NGHỆ THUẬT CHO TÊN VOUCHER --- */
                    .voucher-name { 
                        font-family: 'Dancing Script', cursive; /* Font viết tay nghệ thuật */
                        font-size: 32px; 
                        font-weight: 700; 
                        color: #1a4d2e; 
                        margin: 0; 
                        line-height: 1.2; 
                    }
                
                    .sale-value { font-family: 'Playfair Display', serif; font-size: 60px; font-weight: 900; color: #1a4d2e; margin: 5px 0; line-height: 1; letter-spacing: -1px; }
                    .sale-off-text { font-family: 'Arial', sans-serif; font-size: 18px; font-weight: bold; color: #d4af37; text-transform: uppercase; letter-spacing: 2px; }
                
                    .notch-top { position: absolute; top: -12px; right: -12px; width: 24px; height: 24px; background-color: #eeeeee; border-radius: 50%; z-index: 10; box-shadow: inset 0 -2px 2px rgba(0,0,0,0.1); }
                    .notch-bottom { position: absolute; bottom: -12px; right: -12px; width: 24px; height: 24px; background-color: #eeeeee; border-radius: 50%; z-index: 10; box-shadow: inset 0 2px 2px rgba(0,0,0,0.1); }
                
                    .info-table { width: 100%; margin-top: 15px; font-family: 'Arial', sans-serif; font-size: 13px; color: #555; border-top: 1px solid #eee; padding-top: 10px; clear: both; }
                    .info-label { color: #888; padding: 4px 0; }
                    .info-val { text-align: right; font-weight: 600; color: #333; }
                
                    .btn { display: inline-block; background-color: {{btnColor}}; color: #ffffff; padding: 14px 45px; text-decoration: none; border-radius: 50px; font-weight: bold; margin-top: 25px; font-family: 'Arial', sans-serif; box-shadow: 0 4px 10px rgba(0,0,0,0.2); transition: all 0.3s; }
                    .footer { color: #777; padding: 20px; text-align: center; font-size: 12px; font-family: 'Arial', sans-serif; }
                </style>
                </head>
                <body>
                <div class='container'>
                  <div class='header'>
                    <h1>MY LAPTOP</h1>
                  </div>
                  <div style='padding: 0 10px;'>
                      <p style='margin: 20px 10px; font-family: Arial, sans-serif;'>Xin chào <strong>{{customerName}}</strong>,</p>
                      <p style='margin: 0 10px; color: #555;'>{{messageBody}}</p>
                
                      <div class='voucher-wrapper'>
                        <table class='voucher-table'>
                            <tr>
                                <td class='left-side'>
                                    <div class='notch-top'></div>
                                    <div class='notch-bottom'></div>
                                    <div style='color: #fff; font-size: 24px; font-weight: bold; text-transform: uppercase; letter-spacing: 4px; transform: rotate(-90deg); white-space: nowrap;'>VOUCHER</div>
                                </td>
                                <td class='right-side'>
                                    <img src='{{logoUrl}}' alt='Logo' class='logo-img' />
                                    {{statusBadge}}
                
                                    <div class='voucher-name'>{{voucherName}}</div>
                
                                    <div class='sale-value'>{{discountDisplay}}</div>
                                    <div class='sale-off-text'>SALE OFF</div>
                                    <div style='margin-top: 5px; font-size: 13px; color: #666; font-style: italic;'>
                                        Giảm tối đa: <span style='color: #d32f2f; font-weight: 800;'>{{maxReduceStr}}</span>
                                    </div>
                                    <table class='info-table'>
                                        <tr><td class='info-label'>Mã của bạn:</td><td class='info-val' style='font-size: 16px; color: #218838; font-weight: 800; letter-spacing: 1px;'>{{voucherCode}}</td></tr>
                                        {{infoTableRows}}
                                    </table>
                                </td>
                            </tr>
                        </table>
                      </div>
                      <div style='text-align: center; margin-bottom: 40px;'>
                        <a href='http://localhost:6788/home' class='btn'>{{btnText}}</a>
                      </div>
                  </div>
                  <div class='footer'>
                    <p style='margin: 0;'>Trường Cao Đẳng FPT Polytechnic, Trịnh Văn Bô, Nam Từ Liêm, Hà Nội</p>
                  </div>
                </div>
                </body>
                </html>
                """
                .replace("{{headerColor}}", headerColor)
                .replace("{{customerName}}", customerName)
                .replace("{{messageBody}}", messageBody)
                .replace("{{logoUrl}}", logoUrl)
                .replace("{{statusBadge}}", statusBadge)
                .replace("{{voucherName}}", voucherName)
                .replace("{{discountDisplay}}", discountDisplay)
                .replace("{{maxReduceStr}}", maxReduceStr)
                .replace("{{voucherCode}}", voucherCode)
                .replace("{{infoTableRows}}", infoTableRows)
                .replace("{{btnText}}", btnText)
                .replace("{{btnColor}}", btnColor);
    }

    // --- CÁC HÀM GỌI MAIL ---
    public static String createVoucherEmailBody(Voucher voucher, Customer customer) {
        return generateCommonBody(voucher, customer, "START");
    }

    public static String createPausedEmailBody(Voucher voucher, Customer customer) {
        return generateCommonBody(voucher, customer, "PAUSED");
    }

    public static String createResumedEmailBody(Voucher voucher, Customer customer) {
        return generateCommonBody(voucher, customer, "RESUMED");
    }

    private static String generateCommonBody(Voucher voucher, Customer customer, String type) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        Locale localeVN = new Locale("vi", "VN");
        NumberFormat currencyVN = NumberFormat.getCurrencyInstance(localeVN);

        LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(voucher.getStartDate()), ZoneId.systemDefault());
        LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(voucher.getEndDate()), ZoneId.systemDefault());

        // --- XỬ LÝ SỐ LIỆU (BỎ .00) ---
        String discountDisplay;
        if (voucher.getTypeVoucher() == TypeVoucher.PERCENTAGE) {
            // stripTrailingZeros(): 68.00 -> 68
            discountDisplay = voucher.getDiscountValue().stripTrailingZeros().toPlainString() + "%";
        } else {
            discountDisplay = currencyVN.format(voucher.getDiscountValue());
        }

        String maxReduceStr = currencyVN.format(voucher.getMaxValue());
        String minConditionStr = currencyVN.format(voucher.getConditions());
        String startTimeStr = start.format(dateFormatter);
        String endTimeStr = end.format(dateFormatter);
        String customerName = (customer.getName() != null) ? customer.getName() : "Quý khách";
        String voucherName = (voucher.getName() != null) ? voucher.getName() : "Gift Voucher";
        String logoUrl = "https://i.postimg.cc/cCtdbqwT/logggggggo.png";

        String headerColor = "#218838";
        String messageBody = "";
        String statusBadge = "";
        String infoTableRows = "";
        String btnText = "MUA SẮM NGAY";
        String btnColor = "#218838";

        if (type.equals("PAUSED")) {
            headerColor = "#ff6600"; // Cam
            messageBody = "Chúng tôi thành thật xin lỗi. Voucher <strong>" + voucherName + "</strong> hiện đang tạm ngưng sử dụng do <strong>lỗi hệ thống</strong>. Chúng tôi sẽ khắc phục sớm nhất.";
            statusBadge = "<div style='display: inline-block; background: #ffebee; color: #d32f2f; padding: 4px 12px; border-radius: 20px; font-size: 11px; font-weight: bold; border: 1px solid #ffcdd2; margin-bottom: 5px;'>● TẠM KHÓA</div>";
            btnText = "VỀ TRANG CHỦ";
            btnColor = "#ff6600";

            infoTableRows = "<tr><td class='info-label'>Đơn tối thiểu:</td><td class='info-val'>" + minConditionStr + "</td></tr>" +
                    "<tr><td class='info-label'>Bắt đầu:</td><td class='info-val'>" + startTimeStr + "</td></tr>";
        } else if (type.equals("RESUMED")) {
            headerColor = "#218838"; // Xanh
            messageBody = "Tin vui! Voucher <strong>" + voucherName + "</strong> đã hoạt động trở lại. Số lượng có hạn, sử dụng ngay!";
            statusBadge = "<div style='display: inline-block; background: #e8f5e9; color: #2e7d32; padding: 4px 12px; border-radius: 20px; font-size: 11px; font-weight: bold; border: 1px solid #c8e6c9; margin-bottom: 5px;'>● ĐANG HOẠT ĐỘNG</div>";

            infoTableRows = "<tr><td class='info-label'>Đơn tối thiểu:</td><td class='info-val'>" + minConditionStr + "</td></tr>" +
                    "<tr><td class='info-label'>Số lượng còn:</td><td class='info-val' style='color:#1565c0'>" + voucher.getRemainingQuantity() + "</td></tr>" +
                    "<tr><td class='info-label'>Hạn sử dụng:</td><td class='info-val' style='color:#d32f2f; font-weight:800'>" + endTimeStr + "</td></tr>";
        } else { // START
            headerColor = "#218838";
            messageBody = "Chúng tôi xin gửi tặng bạn một mã giảm giá đặc biệt. Hãy sử dụng ngay để mua sắm thả ga nhé!";
            statusBadge = "<div style='display: inline-block; background: #e8f5e9; color: #2e7d32; padding: 4px 12px; border-radius: 20px; font-size: 11px; font-weight: bold; border: 1px solid #c8e6c9; margin-bottom: 5px;'>● ĐANG HOẠT ĐỘNG</div>";

            infoTableRows = "<tr><td class='info-label'>Đơn tối thiểu:</td><td class='info-val'>" + minConditionStr + "</td></tr>" +
                    "<tr><td class='info-label'>Bắt đầu:</td><td class='info-val'>" + startTimeStr + "</td></tr>" +
                    "<tr><td class='info-label'>Hạn sử dụng:</td><td class='info-val' style='color:#d32f2f; font-weight:800'>" + endTimeStr + "</td></tr>";
        }

        return getTicketHtmlTemplate(headerColor, customerName, messageBody, logoUrl, statusBadge,
                voucherName, discountDisplay, maxReduceStr, voucher.getCode(),
                infoTableRows, btnText, btnColor);
    }

    public static String convertLongToDateString(Long timestamp) {
        // Định dạng ngày tháng bạn mong muốn
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        // Chuyển Long (milliseconds) sang Instant, sau đó áp dụng múi giờ hệ thống
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(formatter);
    }
}