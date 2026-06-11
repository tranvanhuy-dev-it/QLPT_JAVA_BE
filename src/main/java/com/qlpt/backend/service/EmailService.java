package com.qlpt.backend.service;

// Service for sending emails
import com.qlpt.backend.dto.support.ContactRequest;

public interface EmailService {
    public void sendContactEmail(ContactRequest request);
}
