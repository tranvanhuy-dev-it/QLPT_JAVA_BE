package com.qlpt.backend.service.impl;

import com.qlpt.backend.service.EmailService;

import com.qlpt.backend.dto.support.ContactRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.resend.api-key:}")
    private String resendApiKey;

    @Value("${app.resend.from:onboarding@resend.dev}")
    private String fromEmail;

    @Value("${app.resend.admin-email:}")
    private String adminEmail;

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Async
    @Override
    public void sendContactEmail(ContactRequest request) {
        log.info("[EmailService] Bắt đầu gửi email liên hệ từ: {} <{}>", request.getName(), request.getEmail());

        if (resendApiKey == null || resendApiKey.trim().isEmpty()) {
            log.warn("[EmailService] RESEND_API_KEY chưa được cấu hình - chạy chế độ MOCK");
            log.warn("[EmailService] From: {} <{}> | Subject: {}", request.getName(), request.getEmail(), request.getSubject());
            return;
        }

        // 1. Gửi thông báo tới Admin
        String adminTo = (adminEmail != null && !adminEmail.trim().isEmpty()) ? adminEmail : request.getEmail();
        String adminBody = "Chào Admin,\n\n"
                + "Bạn nhận được một yêu cầu liên hệ hỗ trợ mới:\n\n"
                + "- Người gửi: " + request.getName() + "\n"
                + "- Số điện thoại: " + request.getPhone() + "\n"
                + "- Email: " + request.getEmail() + "\n"
                + "- Tiêu đề: " + request.getSubject() + "\n\n"
                + "Nội dung:\n--------------------------------------------------\n"
                + request.getMessage()
                + "\n--------------------------------------------------\n";

        boolean sent = sendViaResend(
                adminTo,
                "[Nhà Trọ Thông Minh] Yêu cầu hỗ trợ: " + request.getSubject(),
                adminBody
        );

        if (sent) {
            log.info("[EmailService] ✅ Đã gửi email thông báo admin thành công!");
        }

        // 2. Gửi auto-reply tới người dùng
        String replyBody = "Xin chào " + request.getName() + ",\n\n"
                + "Cảm ơn bạn đã liên hệ với chúng tôi!\n\n"
                + "Chúng tôi đã nhận được yêu cầu hỗ trợ của bạn với tiêu đề:\n"
                + "\"" + request.getSubject() + "\"\n\n"
                + "Đội ngũ hỗ trợ sẽ phản hồi trong vòng 24 giờ làm việc.\n\n"
                + "Thông tin yêu cầu:\n--------------------------------------------------\n"
                + "Tiêu đề: " + request.getSubject() + "\n"
                + "Nội dung: " + request.getMessage() + "\n"
                + "--------------------------------------------------\n\n"
                + "Trân trọng,\nĐội ngũ Nhà Trọ Thông Minh\n"
                + "--------------------------------------------------\n"
                + "Email này được gửi tự động, vui lòng không trả lời.\n";

        boolean replySent = sendViaResend(
                request.getEmail(),
                "[Nhà Trọ Thông Minh] Đã nhận yêu cầu hỗ trợ của bạn",
                replyBody
        );

        if (replySent) {
            log.info("[EmailService] ✅ Đã gửi auto-reply tới {} thành công!", request.getEmail());
        }
    }

    private boolean sendViaResend(String to, String subject, String textBody) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(resendApiKey);

            Map<String, Object> body = Map.of(
                    "from", fromEmail,
                    "to", List.of(to),
                    "subject", subject,
                    "text", textBody
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(RESEND_API_URL, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[EmailService] Resend API thành công: {}", response.getBody());
                return true;
            } else {
                log.error("[EmailService] Resend API trả về lỗi: {} - {}", response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("[EmailService] ❌ Lỗi gọi Resend API tới {}: {}", to, e.getMessage());
            return false;
        }
    }
}
