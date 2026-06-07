package com.qlpt.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "extra_fees", indexes = {
    @Index(name = "idx_extra_fees_boarding_house_id", columnList = "boarding_house_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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
    @JsonIgnore
    private BoardingHouse boardingHouse;
}
