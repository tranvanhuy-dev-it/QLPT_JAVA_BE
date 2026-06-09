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
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(adminEmail); // Gmail yêu cầu setFrom là tài khoản gửi
            message.setTo(adminEmail);   // Gửi trực tiếp tới hòm thư quản trị
            message.setReplyTo(request.getEmail()); // Tiện cho việc phản hồi trực tiếp qua mail của khách
            message.setSubject("[Nhà Trọ Thông Minh] Yêu cầu hỗ trợ: " + request.getSubject());
            
            String content = "Chào Admin,\n\n"
                    + "Bạn nhận được một yêu cầu liên hệ hỗ trợ mới từ hệ thống:\n\n"
                    + "- Người gửi: " + request.getName() + "\n"
                    + "- Số điện thoại: " + request.getPhone() + "\n"
                    + "- Email liên hệ: " + request.getEmail() + "\n"
                    + "- Tiêu đề: " + request.getSubject() + "\n\n"
                    + "Nội dung chi tiết:\n"
                    + "--------------------------------------------------\n"
                    + request.getMessage() + "\n"
                    + "--------------------------------------------------\n";
            
            message.setText(content);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email hỗ trợ qua SMTP: " + e.getMessage());
        }
    }
}
