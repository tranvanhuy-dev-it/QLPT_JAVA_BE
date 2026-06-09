package com.qlpt.backend.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class BulkInvoiceCreateRequest {
    private LocalDate invoiceDate;
    private List<RoomReading> readings;

    @Data
    public static class RoomReading {
        private UUID roomId;
        private UUID contractId;
        private LocalDate billingPeriodStart;
        private LocalDate billingPeriodEnd;
        private double newElectricityIndex;
        private double newWaterIndex;
        private double discount;
    }
}
