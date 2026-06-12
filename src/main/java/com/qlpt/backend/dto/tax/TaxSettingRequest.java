package com.qlpt.backend.dto.tax;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TaxSettingRequest {

    @Min(value = 0, message = "Ngưỡng chịu thuế không được âm")
    private double annualThreshold;

    @Min(value = 0, message = "Thuế suất GTGT không được âm")
    private double vatRate;

    @Min(value = 0, message = "Thuế suất TNCN không được âm")
    private double pitRate;
}
