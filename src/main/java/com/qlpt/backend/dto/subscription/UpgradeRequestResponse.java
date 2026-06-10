package com.qlpt.backend.dto.subscription;

import com.qlpt.backend.entity.UpgradeRequest;
import java.time.LocalDateTime;
import java.util.UUID;

public record UpgradeRequestResponse(
    UUID id,
    UserMiniResponse user,
    Integer months,
    Double amount,
    String status,
    String paymentContent,
    LocalDateTime createdAt,
    LocalDateTime processedAt
) {
    public record UserMiniResponse(
        UUID id,
        String username,
        String fullName
    ) {}

    public static UpgradeRequestResponse fromEntity(UpgradeRequest request) {
        if (request == null) return null;
        
        UserMiniResponse userMini = null;
        try {
            if (request.getUser() != null) {
                userMini = new UserMiniResponse(
                    request.getUser().getId(),
                    request.getUser().getUsername(),
                    request.getUser().getFullName()
                );
            }
        } catch (org.hibernate.LazyInitializationException e) {
            // If the user entity is not initialized, leave userMini as null
        }

        return new UpgradeRequestResponse(
            request.getId(),
            userMini,
            request.getMonths(),
            request.getAmount(),
            request.getStatus(),
            request.getPaymentContent(),
            request.getCreatedAt(),
            request.getProcessedAt()
        );
    }
}
