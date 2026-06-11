package com.qlpt.backend.entity;
import com.qlpt.backend.enums.Role;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_landlord_id", columnList = "landlord_id"),
    @Index(name = "idx_users_role", columnList = "role")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    private String email;
    private String phone;

    @Column(name = "full_name")
    private String fullName;

    @Column(nullable = false)
    private String status; // ACTIVE, INACTIVE

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id")
    private User landlord; // For TENANTs, references their LANDLORD

    @Column(name = "identity_card")
    private String identityCard;

    @Column(name = "id_card_issue_date")
    private java.time.LocalDate idCardIssueDate;

    @Column(name = "id_card_issue_place")
    private String idCardIssuePlace;

    @Column(name = "permanent_address")
    private String permanentAddress;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "subscription_expired_at")
    private java.time.LocalDate subscriptionExpiredAt;

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "imou_app_id")
    private String imouAppId;

    @Column(name = "imou_app_secret")
    private String imouAppSecret;
}
