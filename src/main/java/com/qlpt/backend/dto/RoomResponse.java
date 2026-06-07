package com.qlpt.backend.dto;

import com.qlpt.backend.entity.Room;
import com.qlpt.backend.entity.RoomStatus;
import java.util.UUID;

public record RoomResponse(
    UUID id,
    String roomNumber,
    double basePrice,
    double currentElectricityIndex,
    double currentWaterIndex,
    int maxPeople,
    RoomStatus status,
    BoardingHouseResponse boardingHouse
) {
    public static RoomResponse fromEntity(Room room) {
        if (room == null) return null;
        try {
            room.getRoomNumber();
        } catch (org.hibernate.LazyInitializationException e) {
            return null;
        }
        return new RoomResponse(
            room.getId(),
            room.getRoomNumber(),
            room.getBasePrice(),
            room.getCurrentElectricityIndex(),
            room.getCurrentWaterIndex(),
            room.getMaxPeople(),
            room.getStatus(),
            BoardingHouseResponse.fromEntity(room.getBoardingHouse())
        );
    }
}
