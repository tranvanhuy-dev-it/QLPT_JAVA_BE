package com.qlpt.backend.entity;
import com.qlpt.backend.enums.RoomStatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "rooms", indexes = {
    @Index(name = "idx_rooms_boarding_house_id", columnList = "boarding_house_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    @Column(name = "base_price", nullable = false)
    private double basePrice;

    @Column(name = "current_electricity_index", nullable = false)
    private double currentElectricityIndex;

    @Column(name = "current_water_index", nullable = false)
    private double currentWaterIndex;

    @Column(name = "max_people", nullable = false)
    private int maxPeople;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status; // VACANT, OCCUPIED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boarding_house_id", nullable = false)
    private BoardingHouse boardingHouse;
}
