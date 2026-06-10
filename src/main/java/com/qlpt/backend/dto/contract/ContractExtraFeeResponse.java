package com.qlpt.backend.dto.contract;
import com.qlpt.backend.dto.extrafee.ExtraFeeResponse;

import com.qlpt.backend.entity.ContractExtraFee;
import java.util.UUID;

public record ContractExtraFeeResponse(
    UUID id,
    ExtraFeeResponse extraFee,
    double customPrice
) {
    public static ContractExtraFeeResponse fromEntity(ContractExtraFee cef) {
        if (cef == null) return null;
        try {
            cef.getCustomPrice();
        } catch (org.hibernate.LazyInitializationException e) {
            return null;
        }
        return new ContractExtraFeeResponse(
            cef.getId(),
            ExtraFeeResponse.fromEntity(cef.getExtraFee()),
            cef.getCustomPrice()
        );
    }
}
