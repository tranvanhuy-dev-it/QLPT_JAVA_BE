package com.qlpt.backend.dto.boardinghouse;

import com.qlpt.backend.entity.BoardingHouseCamera;
import java.time.LocalDateTime;
import java.util.UUID;

public record BoardingHouseCameraResponse(
    UUID id,
    UUID boardingHouseId,
    String name,
    String streamUrl,
    String username,
    String password,
    String brand,
    String serialNumber,
    String safetyCode,
    LocalDateTime createdAt
) {
    public static BoardingHouseCameraResponse fromEntity(BoardingHouseCamera camera) {
        if (camera == null) return null;
        return new BoardingHouseCameraResponse(
            camera.getId(),
            camera.getBoardingHouse() != null ? camera.getBoardingHouse().getId() : null,
            camera.getName(),
            camera.getStreamUrl(),
            camera.getUsername(),
            camera.getPassword(),
            camera.getBrand(),
            camera.getSerialNumber(),
            camera.getSafetyCode(),
            camera.getCreatedAt()
        );
    }
}
