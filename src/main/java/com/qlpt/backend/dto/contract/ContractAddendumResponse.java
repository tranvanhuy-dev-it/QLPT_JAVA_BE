package com.qlpt.backend.dto.contract;

import com.qlpt.backend.entity.ContractAddendum;
import com.qlpt.backend.enums.WaterBillingType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record ContractAddendumResponse(
        UUID id,
        UUID contractId,
        LocalDate startDate,
        double roomPrice,
        double electricityRate,
        double waterRate,
        WaterBillingType waterBillingType,
        int numberOfTenants,
        String description,
        List<ContractAddendumExtraFeeResponse> extraFees) {
    public static ContractAddendumResponse fromEntity(ContractAddendum addendum) {
        if (addendum == null)
            return null;
        List<ContractAddendumExtraFeeResponse> fees = null;
        try {
            if (addendum.getExtraFees() != null) {
                fees = addendum.getExtraFees().stream()
                        .map(ContractAddendumExtraFeeResponse::fromEntity)
                        .collect(Collectors.toList());
            }
        } catch (org.hibernate.LazyInitializationException e) {
            // keep null
        }
        return new ContractAddendumResponse(
                addendum.getId(),
                addendum.getContract().getId(),
                addendum.getStartDate(),
                addendum.getRoomPrice(),
                addendum.getElectricityRate(),
                addendum.getWaterRate(),
                addendum.getWaterBillingType(),
                addendum.getNumberOfTenants(),
                addendum.getDescription(),
                fees);
    }
}
