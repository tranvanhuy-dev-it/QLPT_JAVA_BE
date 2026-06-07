package com.qlpt.backend.dto;

import com.qlpt.backend.entity.BillingMode;
import com.qlpt.backend.entity.Contract;
import com.qlpt.backend.entity.ContractStatus;
import java.time.LocalDate;
import java.util.UUID;

public record ContractResponse(
    UUID id,
    LocalDate startDate,
    LocalDate endDate,
    double deposit,
    double contractedRoomPrice,
    BillingMode billingMode,
    Integer fixedBillingDay,
    ContractStatus status,
    RoomResponse room,
    UserResponse tenant,
    int numberOfTenants
) {
    public static ContractResponse fromEntity(Contract contract) {
        if (contract == null || !org.hibernate.Hibernate.isInitialized(contract)) return null;
        return new ContractResponse(
            contract.getId(),
            contract.getStartDate(),
            contract.getEndDate(),
            contract.getDeposit(),
            contract.getContractedRoomPrice(),
            contract.getBillingMode(),
            contract.getFixedBillingDay(),
            contract.getStatus(),
            RoomResponse.fromEntity(contract.getRoom()),
            UserResponse.fromEntity(contract.getTenant()),
            contract.getNumberOfTenants()
        );
    }
}
