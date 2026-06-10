package com.qlpt.backend.service;

import com.qlpt.backend.dto.support.ContactRequest;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

public interface EmailService {
    public void sendContactEmail(ContactRequest request);
}
