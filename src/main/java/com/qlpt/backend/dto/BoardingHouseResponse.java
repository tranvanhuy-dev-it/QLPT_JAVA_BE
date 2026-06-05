package com.qlpt.backend.dto;

import com.qlpt.backend.entity.BoardingHouse;
import com.qlpt.backend.entity.WaterBillingType;
import java.util.UUID;

public record BoardingHouseResponse(
    UUID id,
    String name,
    String address,
    double defaultElectricityRate,
    double defaultWaterRate,
    WaterBillingType waterBillingType
) {
    public static BoardingHouseResponse fromEntity(BoardingHouse bh) {
        if (bh == null) return null;
        return new BoardingHouseResponse(
            bh.getId(),
            bh.getName(),
            bh.getAddress(),
            bh.getDefaultElectricityRate(),
            bh.getDefaultWaterRate(),
            bh.getWaterBillingType()
        );
    }
}
