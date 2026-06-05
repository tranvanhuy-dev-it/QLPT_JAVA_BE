package com.qlpt.backend.dto;

import com.qlpt.backend.entity.BillingMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class ContractCreateRequest {

    @NotNull(message = "ID phòng không được trống")
    private UUID roomId;

    @NotNull(message = "ID người thuê không được trống")
    private UUID tenantId;

    @NotNull(message = "Ngày bắt đầu thuê không được trống")
    private LocalDate startDate;

    private LocalDate endDate;

    @Min(value = 0, message = "Tiền cọc không được âm")
    private double deposit;

    @Min(value = 0, message = "Giá thuê phòng không được âm")
    private double contractedRoomPrice;

    @NotNull(message = "Chế độ tính tiền không được trống")
    private BillingMode billingMode; // BY_RENTAL_DAYS, FIXED_DATE_OF_MONTH

    private Integer fixedBillingDay; // Bắt buộc nếu chọn FIXED_DATE_OF_MONTH

    @Min(value = 1, message = "Số người ở phải lớn hơn hoặc bằng 1")
    private int numberOfTenants;

    private List<ExtraFeeOverride> extraFees;

    @Data
    public static class ExtraFeeOverride {
        @NotNull(message = "ID dịch vụ phụ phí không được trống")
        private UUID extraFeeId;

        @Min(value = 0, message = "Giá phụ phí không được âm")
        private double customPrice;
    }
}
