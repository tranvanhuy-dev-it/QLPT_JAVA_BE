package com.qlpt.backend.dto.extrafee;

import com.qlpt.backend.entity.ExtraFee;
import com.qlpt.backend.enums.ExtraFeeUnitType;
import java.util.UUID;

public record ExtraFeeResponse(
    UUID id,
    String name,
    double defaultPrice,
    ExtraFeeUnitType unitType
) {
    public static ExtraFeeResponse fromEntity(ExtraFee fee) {
        if (fee == null) return null;
        try {
            fee.getName();
        } catch (org.hibernate.LazyInitializationException e) {
            return null;
        }
        return new ExtraFeeResponse(
            fee.getId(),
            fee.getName(),
            fee.getDefaultPrice(),
            fee.getUnitType()
        );
    }
}
