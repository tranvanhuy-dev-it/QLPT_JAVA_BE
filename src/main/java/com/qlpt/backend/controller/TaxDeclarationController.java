package com.qlpt.backend.controller;

import com.qlpt.backend.config.CustomUserDetails;
import com.qlpt.backend.dto.tax.TaxDeclarationRequest;
import com.qlpt.backend.dto.tax.TaxDeclarationResponse;
import com.qlpt.backend.dto.tax.TaxSettingRequest;
import com.qlpt.backend.entity.TaxSetting;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.service.TaxDeclarationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tax")
@PreAuthorize("hasRole('LANDLORD')")
public class TaxDeclarationController {

    private final TaxDeclarationService taxDeclarationService;

    public TaxDeclarationController(TaxDeclarationService taxDeclarationService) {
        this.taxDeclarationService = taxDeclarationService;
    }

    @GetMapping("/setting")
    public ResponseEntity<TaxSetting> getSetting(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(taxDeclarationService.getTaxSetting(landlord));
    }

    @PutMapping("/setting")
    public ResponseEntity<TaxSetting> updateSetting(
            @Valid @RequestBody TaxSettingRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(taxDeclarationService.updateTaxSetting(landlord, request));
    }

    @GetMapping
    public ResponseEntity<List<TaxDeclarationResponse>> getDeclarations(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(taxDeclarationService.getDeclarations(landlord));
    }

    @PostMapping("/calculate")
    public ResponseEntity<TaxDeclarationResponse> calculate(
            @Valid @RequestBody TaxDeclarationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(taxDeclarationService.calculateTax(landlord, request));
    }

    @PostMapping("/declare")
    public ResponseEntity<TaxDeclarationResponse> declare(
            @Valid @RequestBody TaxDeclarationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        return ResponseEntity.ok(taxDeclarationService.submitDeclaration(landlord, request));
    }

    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) UUID boardingHouseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws IOException {
        User landlord = userDetails.getUser();
        byte[] excelBytes = taxDeclarationService.exportExcel(landlord, boardingHouseId, startDate, endDate);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bao_cao_doanh_thu.xlsx\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelBytes);
    }
}
