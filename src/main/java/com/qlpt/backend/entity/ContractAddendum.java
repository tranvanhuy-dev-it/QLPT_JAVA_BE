package com.qlpt.backend.entity;
import com.qlpt.backend.enums.WaterBillingType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "contract_addendums", indexes = {
    @Index(name = "idx_addendums_contract_id", columnList = "contract_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ContractAddendum {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate; // Ngày hiệu lực của phụ lục

    @Column(name = "room_price", nullable = false)
    private double roomPrice;

    @Column(name = "electricity_rate", nullable = false)
    private double electricityRate;

    @Column(name = "water_rate", nullable = false)
    private double waterRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "water_billing_type", nullable = false)
    private WaterBillingType waterBillingType;

    @Column(name = "number_of_tenants", nullable = false)
    private int numberOfTenants;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "addendum", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ContractAddendumExtraFee> extraFees = new ArrayList<>();
}
