package com.qlpt.backend.dto.contract;
import com.qlpt.backend.dto.extrafee.ExtraFeeResponse;

import com.qlpt.backend.entity.ContractAddendumExtraFee;
import java.util.UUID;

public record ContractAddendumExtraFeeResponse(
    UUID id,
    double customPrice,
    ExtraFeeResponse extraFee
) {
    public static ContractAddendumExtraFeeResponse fromEntity(ContractAddendumExtraFee aef) {
        if (aef == null) return null;
        return new ContractAddendumExtraFeeResponse(
            aef.getId(),
            aef.getCustomPrice(),
            ExtraFeeResponse.fromEntity(aef.getExtraFee())
        );
    }
}
