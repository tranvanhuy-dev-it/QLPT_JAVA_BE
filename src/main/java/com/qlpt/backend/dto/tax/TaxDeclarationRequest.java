package com.qlpt.backend.dto.tax;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxDeclarationRequest {

    @Min(value = 2000, message = "Năm không hợp lệ")
    private int year;

    @NotBlank(message = "Loại kỳ không được để trống")
    private String periodType; // "MONTH", "QUARTER", "YEAR"

    @Min(value = 0, message = "Giá trị kỳ không hợp lệ")
    private int periodValue; // Month (1-12), Quarter (1-4), or 0 for year

    private UUID boardingHouseId; // Null if for all boarding houses
}
