package com.qlpt.backend.controller;

import com.qlpt.backend.dto.ContactRequest;
import com.qlpt.backend.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final EmailService emailService;

    public SupportController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/contact")
    public ResponseEntity<Void> sendContact(@Valid @RequestBody ContactRequest request) {
        emailService.sendContactEmail(request);
        return ResponseEntity.ok().build();
    }
}
