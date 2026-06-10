package com.qlpt.backend.dto.room;
import com.qlpt.backend.dto.boardinghouse.BoardingHouseResponse;

import com.qlpt.backend.entity.Room;
import com.qlpt.backend.enums.RoomStatus;
import com.qlpt.backend.entity.ContractAddendum;
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
        return fromEntity(room, null);
    }

    public static RoomResponse fromEntity(Room room, ContractAddendum latest) {
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
            BoardingHouseResponse.fromEntity(room.getBoardingHouse(), latest)
        );
    }
}
