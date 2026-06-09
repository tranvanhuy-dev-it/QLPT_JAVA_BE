package com.qlpt.backend.service;

import com.qlpt.backend.dto.ContactRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String adminEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendContactEmail(ContactRequest request) {
        log.info("[EmailService] Bắt đầu gửi email liên hệ từ: {} <{}>", request.getName(), request.getEmail());
        log.info("[EmailService] adminEmail được cấu hình: '{}'", adminEmail);

        // Kiểm tra xem đã cấu hình email thật chưa
        if (adminEmail == null || adminEmail.equals("your_email@gmail.com") || adminEmail.trim().isEmpty()) {
            log.warn("[EmailService] SMTP chưa được cấu hình - chạy chế độ MOCK");
            log.warn("[EmailService] From: {} <{}> | Subject: {}", request.getName(), request.getEmail(), request.getSubject());
            return;
        }

        try {
            log.info("[EmailService] Đang gửi email thông báo tới admin: {}", adminEmail);

            // 1. Gửi thông báo tới Admin
            SimpleMailMessage adminMessage = new SimpleMailMessage();
            adminMessage.setFrom(adminEmail);
            adminMessage.setTo(adminEmail);
            adminMessage.setReplyTo(request.getEmail());
            adminMessage.setSubject("[Nhà Trọ Thông Minh] Yêu cầu hỗ trợ: " + request.getSubject());

            String adminContent = "Chào Admin,\n\n"
                    + "Bạn nhận được một yêu cầu liên hệ hỗ trợ mới từ hệ thống:\n\n"
                    + "- Người gửi: " + request.getName() + "\n"
                    + "- Số điện thoại: " + request.getPhone() + "\n"
                    + "- Email liên hệ: " + request.getEmail() + "\n"
                    + "- Tiêu đề: " + request.getSubject() + "\n\n"
                    + "Nội dung chi tiết:\n"
                    + "--------------------------------------------------\n"
                    + request.getMessage() + "\n"
                    + "--------------------------------------------------\n";

            adminMessage.setText(adminContent);
            mailSender.send(adminMessage);
            log.info("[EmailService] ✅ Đã gửi email thông báo admin thành công!");

            // 2. Gửi email phản hồi tự động tới người dùng
            sendAutoReplyEmail(request);

        } catch (Exception e) {
            log.error("[EmailService] ❌ Lỗi khi gửi email SMTP: {}", e.getMessage(), e);
        }
    }

    private void sendAutoReplyEmail(ContactRequest request) {
        try {
            log.info("[EmailService] Đang gửi auto-reply tới: {}", request.getEmail());

            SimpleMailMessage replyMessage = new SimpleMailMessage();
            replyMessage.setFrom(adminEmail);
            replyMessage.setTo(request.getEmail());
            replyMessage.setSubject("[Nhà Trọ Thông Minh] Đã nhận yêu cầu hỗ trợ của bạn");

            String replyContent = "Xin chào " + request.getName() + ",\n\n"
                    + "Cảm ơn bạn đã liên hệ với chúng tôi!\n\n"
                    + "Chúng tôi đã nhận được yêu cầu hỗ trợ của bạn với tiêu đề:\n"
                    + "\"" + request.getSubject() + "\"\n\n"
                    + "Đội ngũ hỗ trợ của Nhà Trọ Thông Minh sẽ xem xét và phản hồi bạn "
                    + "trong vòng 24 giờ làm việc qua địa chỉ email này.\n\n"
                    + "Thông tin yêu cầu của bạn:\n"
                    + "--------------------------------------------------\n"
                    + "Tiêu đề: " + request.getSubject() + "\n"
                    + "Nội dung: " + request.getMessage() + "\n"
                    + "--------------------------------------------------\n\n"
                    + "Nếu bạn cần hỗ trợ khẩn cấp, vui lòng liên hệ trực tiếp qua số điện thoại "
                    + "được cung cấp trên trang web của chúng tôi.\n\n"
                    + "Trân trọng,\n"
                    + "Đội ngũ Nhà Trọ Thông Minh\n"
                    + "--------------------------------------------------\n"
                    + "Email này được gửi tự động, vui lòng không trả lời trực tiếp.\n";

            replyMessage.setText(replyContent);
            mailSender.send(replyMessage);
            log.info("[EmailService] ✅ Đã gửi auto-reply tới {} thành công!", request.getEmail());

        } catch (Exception e) {
            log.error("[EmailService] ❌ Không thể gửi auto-reply tới {}: {}", request.getEmail(), e.getMessage(), e);
        }
    }
}
