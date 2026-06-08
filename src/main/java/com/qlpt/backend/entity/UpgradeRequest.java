package com.qlpt.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "upgrade_requests", indexes = {
    @Index(name = "idx_upgrade_requests_user_id", columnList = "user_id"),
    @Index(name = "idx_upgrade_requests_status", columnList = "status"),
    @Index(name = "idx_upgrade_requests_payment_content", columnList = "payment_content")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpgradeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer months;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "payment_content", nullable = false, unique = true)
    private String paymentContent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
