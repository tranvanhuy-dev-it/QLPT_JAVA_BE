package com.qlpt.backend.dto.boardinghouse;
import com.qlpt.backend.dto.user.UserResponse;
import com.qlpt.backend.dto.extrafee.ExtraFeeResponse;

import com.qlpt.backend.entity.BoardingHouse;
import com.qlpt.backend.enums.WaterBillingType;
import com.qlpt.backend.entity.ContractAddendum;
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
    String bankName,
    String bankAccountNumber,
    String bankAccountName,
    String rules,
    Integer fixedBillingDay
) {
    public static BoardingHouseResponse fromEntity(BoardingHouse bh) {
        return fromEntity(bh, null);
    }

    public static BoardingHouseResponse fromEntity(BoardingHouse bh, ContractAddendum latest) {
        if (bh == null) return null;
        try {
            bh.getName();
        } catch (org.hibernate.LazyInitializationException e) {
            return null;
        }
        List<ExtraFeeResponse> fees = null;
        try {
            if (bh.getExtraFees() != null) {
                fees = bh.getExtraFees().stream()
                    .map(ExtraFeeResponse::fromEntity)
                    .collect(Collectors.toList());
            }
        } catch (org.hibernate.LazyInitializationException e) {
            // keep null
        }

        double elecRate = latest != null ? latest.getElectricityRate() : bh.getDefaultElectricityRate();
        double waterRate = latest != null ? latest.getWaterRate() : bh.getDefaultWaterRate();
        WaterBillingType waterType = latest != null ? latest.getWaterBillingType() : bh.getWaterBillingType();

        return new BoardingHouseResponse(
            bh.getId(),
            bh.getName(),
            bh.getAddress(),
            elecRate,
            waterRate,
            waterType,
            fees,
            UserResponse.fromEntity(bh.getLandlord()),
            bh.getBankName(),
            bh.getBankAccountNumber(),
            bh.getBankAccountName(),
            bh.getRules(),
            bh.getFixedBillingDay()
        );
    }
}
