package com.qlpt.backend.service;

import com.qlpt.backend.dto.tax.TaxDeclarationRequest;
import com.qlpt.backend.dto.tax.TaxDeclarationResponse;
import com.qlpt.backend.dto.tax.TaxSettingRequest;
import com.qlpt.backend.entity.TaxSetting;
import com.qlpt.backend.entity.User;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TaxDeclarationService {
    TaxSetting getTaxSetting(User landlord);
    TaxSetting updateTaxSetting(User landlord, TaxSettingRequest request);
    List<TaxDeclarationResponse> getDeclarations(User landlord);
    TaxDeclarationResponse calculateTax(User landlord, TaxDeclarationRequest request);
    TaxDeclarationResponse submitDeclaration(User landlord, TaxDeclarationRequest request);
    byte[] exportExcel(User landlord, UUID boardingHouseId, LocalDate start, LocalDate end) throws IOException;
}
