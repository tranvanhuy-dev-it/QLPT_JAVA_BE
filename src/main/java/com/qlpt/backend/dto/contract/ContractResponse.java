package com.qlpt.backend.dto.contract;
import com.qlpt.backend.dto.user.UserResponse;
import com.qlpt.backend.dto.room.RoomResponse;

import com.qlpt.backend.entity.Contract;
import com.qlpt.backend.enums.ContractStatus;
import com.qlpt.backend.entity.ContractAddendum;
import java.time.LocalDate;
import java.util.UUID;

public record ContractResponse(
    UUID id,
    LocalDate startDate,
    LocalDate endDate,
    double deposit,
    double contractedRoomPrice,
    ContractStatus status,
    RoomResponse room,
    UserResponse tenant,
    int numberOfTenants,
    Integer fixedBillingDay
) {
    public static ContractResponse fromEntity(Contract contract) {
        if (contract == null) return null;
        try {
            contract.getStartDate();
        } catch (org.hibernate.LazyInitializationException e) {
            return null;
        }

        ContractAddendum latest = contract.getLatestAddendum();
        double roomPrice = latest != null ? latest.getRoomPrice() : contract.getContractedRoomPrice();
        int tenants = latest != null ? latest.getNumberOfTenants() : contract.getNumberOfTenants();
        RoomResponse roomResponse = RoomResponse.fromEntity(contract.getRoom(), latest);

        return new ContractResponse(
            contract.getId(),
            contract.getStartDate(),
            contract.getEndDate(),
            contract.getDeposit(),
            roomPrice,
            contract.getStatus(),
            roomResponse,
            UserResponse.fromEntity(contract.getTenant()),
            tenants,
            contract.getFixedBillingDay()
        );
    }
}
