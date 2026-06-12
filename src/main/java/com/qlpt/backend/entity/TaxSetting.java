package com.qlpt.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "tax_settings", indexes = {
    @Index(name = "idx_tax_settings_landlord_id", columnList = "landlord_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TaxSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false, unique = true)
    private User landlord;

    @Column(name = "annual_threshold", nullable = false)
    private double annualThreshold; // default 500,000,000 VND

    @Column(name = "vat_rate", nullable = false)
    private double vatRate; // default 5.0%

    @Column(name = "pit_rate", nullable = false)
    private double pitRate; // default 5.0%
}
