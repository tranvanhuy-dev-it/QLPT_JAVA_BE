package com.qlpt.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "extra_fees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtraFee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "default_price", nullable = false)
    private double defaultPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", nullable = false)
    private ExtraFeeUnitType unitType; // FIXED_PER_ROOM, FIXED_PER_PERSON

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boarding_house_id", nullable = false)
    private BoardingHouse boardingHouse;
}
