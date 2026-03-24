package com.sd20201.datn.infrastructure.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;

import org.thymeleaf.context.Context;

@Service
@EnableAsync
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;
    @Autowired
    private JavaMailSender javaMailSender;

    @Async
    public void sendNewStaffEmail(String toEmail, String fullName, String staffCode, String username, String password) throws MessagingException {
        Context context = new Context();
        context.setVariable("fullName", fullName);
        context.setVariable("staffCode", staffCode);
        context.setVariable("username", username);
        context.setVariable("password", password);

        String html = templateEngine.process("new_staff_email", context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(toEmail);
        helper.setSubject("Thông tin tài khoản mới");
        helper.setText(html, true); // HTML
        mailSender.send(message);
    }

    public void sendResetPasswordEmail(String toEmail, String fullName, String newPassword) throws MessagingException {
        // Tạo một MimeMessage để có thể gửi mail dạng HTML (đẹp hơn mail text bình thường)
        MimeMessage message = javaMailSender.createMimeMessage();

        // true: biểu thị mail này có chứa multipart (HTML), "UTF-8" để không bị lỗi font tiếng Việt
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail);
        helper.setSubject("🔒 MY LAPTOP - Cấp lại mật khẩu tài khoản");

        // Thiết kế giao diện Email bằng HTML/CSS
        String htmlContent = "<div style='font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);'>" + "<div style='background-color: #16a34a; padding: 25px; text-align: center;'>" + "  <h1 style='margin: 0; color: #ffffff; font-size: 24px; letter-spacing: 1px;'>MY LAPTOP</h1>" + "</div>" + "<div style='padding: 30px;'>" + "  <p style='font-size: 16px;'>Chào <strong>" + fullName + "</strong>,</p>" + "  <p style='font-size: 16px;'>Chúng tôi nhận được yêu cầu cấp lại mật khẩu cho tài khoản của bạn tại hệ thống MY LAPTOP.</p>" + "  <div style='background-color: #f3f4f6; padding: 20px; border-radius: 8px; text-align: center; margin: 25px 0; border: 1px dashed #16a34a;'>" + "    <p style='margin: 0; font-size: 14px; color: #6b7280; text-transform: uppercase; font-weight: bold;'>Mật khẩu mới của bạn là:</p>" + "    <h2 style='margin: 10px 0 0 0; color: #16a34a; font-size: 32px; letter-spacing: 3px;'>" + newPassword + "</h2>" + "  </div>" + "  <p style='color: #dc2626; font-size: 15px; border-left: 4px solid #dc2626; padding-left: 10px; margin-bottom: 25px;'>" + "    <strong>⚠️ Lưu ý quan trọng:</strong> Vui lòng đăng nhập và tiến hành đổi lại mật khẩu ngay lập tức để đảm bảo an toàn cho tài khoản của bạn." + "  </p>" + "  <p style='font-size: 14px; color: #6b7280;'>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này hoặc liên hệ ngay với bộ phận CSKH của chúng tôi.</p>" + "  <hr style='border: none; border-top: 1px solid #e5e7eb; margin: 25px 0;'>" + "  <p style='margin: 0; font-size: 15px;'>Trân trọng,</p>" + "  <p style='margin: 5px 0 0 0; font-size: 16px;'><strong>Đội ngũ MY LAPTOP</strong></p>" + "</div>" + "</div>";

        // true ở tham số thứ 2 xác nhận nội dung truyền vào là HTML
        helper.setText(htmlContent, true);

        // Thực hiện gửi
        javaMailSender.send(message);
    }

}

