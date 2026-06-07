package com.qlpt.backend.dto;

import com.qlpt.backend.entity.ExtraFee;
import com.qlpt.backend.entity.ExtraFeeUnitType;
import java.util.UUID;

public record ExtraFeeResponse(
    UUID id,
    String name,
    double defaultPrice,
    ExtraFeeUnitType unitType
) {
    public static ExtraFeeResponse fromEntity(ExtraFee fee) {
        if (fee == null || !org.hibernate.Hibernate.isInitialized(fee)) return null;
        return new ExtraFeeResponse(
            fee.getId(),
            fee.getName(),
            fee.getDefaultPrice(),
            fee.getUnitType()
        );
    }
}
