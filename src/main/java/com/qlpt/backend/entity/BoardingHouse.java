package com.qlpt.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

@Entity
@Table(name = "boarding_houses", indexes = {
    @Index(name = "idx_boarding_houses_landlord_id", columnList = "landlord_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class BoardingHouse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String address;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_account_name")
    private String bankAccountName;

    @Column(name = "default_electricity_rate", nullable = false)
    private double defaultElectricityRate; // e.g. 3500 VND/kWh

    @Column(name = "default_water_rate", nullable = false)
    private double defaultWaterRate; // e.g. 15000 VND/m3 or 100000 VND/person

    @Enumerated(EnumType.STRING)
    @Column(name = "water_billing_type", nullable = false)
    private WaterBillingType waterBillingType; // BY_INDEX, FIXED_PER_PERSON

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    @OneToMany(mappedBy = "boardingHouse", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ExtraFee> extraFees = new ArrayList<>();
}
