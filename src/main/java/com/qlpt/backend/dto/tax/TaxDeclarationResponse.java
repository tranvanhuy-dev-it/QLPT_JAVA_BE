package com.qlpt.backend.dto.tax;

import com.qlpt.backend.entity.TaxDeclaration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxDeclarationResponse {

    private UUID id;
    private UUID boardingHouseId;
    private String boardingHouseName;
    private int year;
    private String periodType;
    private int periodValue;
    private String periodLabel;
    
    private double totalRevenue;
    private double vatAmount;
    private double pitAmount;
    private double totalTaxAmount;
    
    private String declarationNumber;
    private String status;
    private LocalDateTime submittedDate;
    private String taxAuthorityResponse;
    
    private boolean isTaxable;
    private double annualRevenueSoFar;
    private double annualThreshold;

    public static TaxDeclarationResponse fromEntity(TaxDeclaration entity, double annualRevenue, double threshold) {
        String label = "";
        if ("MONTH".equals(entity.getPeriodType())) {
            label = "Tháng " + entity.getPeriodValue() + "/" + entity.getYear();
        } else if ("QUARTER".equals(entity.getPeriodType())) {
            label = "Quý " + convertToRoman(entity.getPeriodValue()) + "/" + entity.getYear();
        } else {
            label = "Năm " + entity.getYear();
        }

        return TaxDeclarationResponse.builder()
                .id(entity.getId())
                .boardingHouseId(entity.getBoardingHouse() != null ? entity.getBoardingHouse().getId() : null)
                .boardingHouseName(entity.getBoardingHouse() != null ? entity.getBoardingHouse().getName() : "Tất cả dãy trọ")
                .year(entity.getYear())
                .periodType(entity.getPeriodType())
                .periodValue(entity.getPeriodValue())
                .periodLabel(label)
                .totalRevenue(entity.getTotalRevenue())
                .vatAmount(entity.getVatAmount())
                .pitAmount(entity.getPitAmount())
                .totalTaxAmount(entity.getTotalTaxAmount())
                .declarationNumber(entity.getDeclarationNumber())
                .status(entity.getStatus())
                .submittedDate(entity.getSubmittedDate())
                .taxAuthorityResponse(entity.getTaxAuthorityResponse())
                .isTaxable(entity.getTotalTaxAmount() > 0)
                .annualRevenueSoFar(annualRevenue)
                .annualThreshold(threshold)
                .build();
    }

    private static String convertToRoman(int number) {
        switch (number) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            default: return String.valueOf(number);
        }
    }
}
