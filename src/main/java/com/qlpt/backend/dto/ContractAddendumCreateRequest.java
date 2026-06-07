package com.qlpt.backend.dto;

import com.qlpt.backend.entity.WaterBillingType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class ContractAddendumCreateRequest {

    @NotNull(message = "Ngày bắt đầu hiệu lực không được trống")
    private LocalDate startDate;

    @Min(value = 0, message = "Giá thuê phòng không được âm")
    private double roomPrice;

    @Min(value = 0, message = "Giá điện không được âm")
    private double electricityRate;

    @Min(value = 0, message = "Giá nước không được âm")
    private double waterRate;

    @NotNull(message = "Cách tính tiền nước không được trống")
    private WaterBillingType waterBillingType;

    @Min(value = 1, message = "Số người ở phải lớn hơn hoặc bằng 1")
    private int numberOfTenants;

    private String description;

    private List<ExtraFeeOverride> extraFees;

    @Data
    public static class ExtraFeeOverride {
        @NotNull(message = "ID dịch vụ phụ phí không được trống")
        private UUID extraFeeId;

        @Min(value = 0, message = "Giá phụ phí không được âm")
        private double customPrice;
    }
}
