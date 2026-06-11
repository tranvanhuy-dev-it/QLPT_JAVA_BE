package com.qlpt.backend.service;

import com.qlpt.backend.dto.support.ContactRequest;

public interface EmailService {
    public void sendContactEmail(ContactRequest request);
}
