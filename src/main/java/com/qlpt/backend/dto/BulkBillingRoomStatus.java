package com.qlpt.backend.dto;

import com.qlpt.backend.entity.RoomStatus;
import com.qlpt.backend.entity.WaterBillingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkBillingRoomStatus {
    private UUID roomId;
    private String roomNumber;
    private RoomStatus roomStatus;
    private double currentElectricityIndex;
    private double currentWaterIndex;
    private boolean hasActiveContract;
    private UUID contractId;
    private String tenantName;
    private LocalDate nextBillingPeriodStart;
    private LocalDate nextBillingPeriodEnd;
    private LocalDate contractStartDate;
    private Integer fixedBillingDay;
    private double defaultElectricityRate;
    private double defaultWaterRate;
    private WaterBillingType waterBillingType;
}
