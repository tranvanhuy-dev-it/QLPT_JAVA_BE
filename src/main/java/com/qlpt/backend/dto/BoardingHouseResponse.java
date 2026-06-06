package com.qlpt.backend.dto;

import com.qlpt.backend.entity.BoardingHouse;
import com.qlpt.backend.entity.WaterBillingType;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record BoardingHouseResponse(
    UUID id,
    String name,
    String address,
    double defaultElectricityRate,
    double defaultWaterRate,
    WaterBillingType waterBillingType,
    List<ExtraFeeResponse> extraFees,
    UserResponse landlord,
    com.qlpt.backend.entity.BillingTiming billingTiming
) {
    public static BoardingHouseResponse fromEntity(BoardingHouse bh) {
        if (bh == null) return null;
        List<ExtraFeeResponse> fees = null;
        if (bh.getExtraFees() != null) {
            fees = bh.getExtraFees().stream()
                .map(ExtraFeeResponse::fromEntity)
                .collect(Collectors.toList());
        }
        return new BoardingHouseResponse(
            bh.getId(),
            bh.getName(),
            bh.getAddress(),
            bh.getDefaultElectricityRate(),
            bh.getDefaultWaterRate(),
            bh.getWaterBillingType(),
            fees,
            UserResponse.fromEntity(bh.getLandlord()),
            bh.getBillingTiming()
        );
    }
}
