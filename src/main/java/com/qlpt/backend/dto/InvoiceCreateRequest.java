package com.qlpt.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class InvoiceCreateRequest {

    @NotNull(message = "ID hợp đồng không được trống")
    private UUID contractId;

    @NotNull(message = "Ngày tạo hóa đơn không được trống")
    private LocalDate invoiceDate;

    @NotNull(message = "Ngày bắt đầu kỳ thanh toán không được trống")
    private LocalDate billingPeriodStart;

    @NotNull(message = "Ngày kết thúc kỳ thanh toán không được trống")
    private LocalDate billingPeriodEnd;

    private double newElectricityIndex; // Chỉ số điện mới (tháng này)

    private double newWaterIndex; // Chỉ số nước mới (tháng này - có thể bằng 0 nếu tính tiền nước cố định)

    private Double discount; // Số tiền giảm giá cho khách thuê
}
