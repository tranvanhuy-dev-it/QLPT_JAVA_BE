package com.qlpt.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {

    @Id
    private Long id; // We will use a fixed ID = 1 for the single system setting record

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_account")
    private String bankAccount;

    @Column(name = "account_name")
    private String accountName;

    private String phone;
    private String email;

    @Column(name = "full_name")
    private String fullName;
}
