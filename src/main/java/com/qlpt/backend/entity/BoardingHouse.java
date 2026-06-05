package com.qlpt.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "boarding_houses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardingHouse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String address;

    @Column(name = "default_electricity_rate", nullable = false)
    private double defaultElectricityRate; // e.g. 3500 VND/kWh

    @Column(name = "default_water_rate", nullable = false)
    private double defaultWaterRate; // e.g. 15000 VND/m3 or 100000 VND/person

    @Enumerated(EnumType.STRING)
    @Column(name = "water_billing_type", nullable = false)
    private WaterBillingType waterBillingType; // BY_INDEX, FIXED_PER_PERSON, FIXED_PER_ROOM

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;
}
