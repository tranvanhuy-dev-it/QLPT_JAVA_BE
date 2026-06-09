package com.qlpt.backend.service;

import com.qlpt.backend.dto.ContactRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String adminEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendContactEmail(ContactRequest request) {
        // Kiểm tra xem đã cấu hình email thật chưa (nếu vẫn để giá trị mặc định hoặc trống thì chạy giả lập)
        if (adminEmail == null || adminEmail.equals("your_email@gmail.com") || adminEmail.trim().isEmpty()) {
            System.out.println("=== [SMTP MOCK EMAIL SENDER] ===");
            System.out.println("Admin Email: (Chưa cấu hình thực tế trong Backend)");
            System.out.println("From Name:   " + request.getName());
            System.out.println("From Email:  " + request.getEmail());
            System.out.println("From Phone:  " + request.getPhone());
            System.out.println("Subject:     " + request.getSubject());
            System.out.println("Message:     " + request.getMessage());
            System.out.println("=================================");
            System.out.println("=== [SMTP MOCK AUTO-REPLY] ===");
            System.out.println("To:          " + request.getEmail());
            System.out.println("Subject:     [Nhà Trọ Thông Minh] Đã nhận yêu cầu hỗ trợ của bạn");
            System.out.println("==============================");
            return;
        }

        try {
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

            // 2. Gửi email phản hồi tự động tới người dùng
            sendAutoReplyEmail(request);

        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email hỗ trợ qua SMTP: " + e.getMessage());
        }
    }

    private void sendAutoReplyEmail(ContactRequest request) {
        try {
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
        } catch (Exception e) {
            // Ghi log nhưng không ném lỗi để không ảnh hưởng luồng chính
            System.err.println("[EmailService] Không thể gửi auto-reply tới " + request.getEmail() + ": " + e.getMessage());
        }
    }
}
