package com.qlpt.backend.service.impl;

import com.qlpt.backend.dto.tax.TaxDeclarationRequest;
import com.qlpt.backend.dto.tax.TaxDeclarationResponse;
import com.qlpt.backend.dto.tax.TaxSettingRequest;
import com.qlpt.backend.entity.BoardingHouse;
import com.qlpt.backend.entity.Invoice;
import com.qlpt.backend.entity.TaxDeclaration;
import com.qlpt.backend.entity.TaxSetting;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.repository.BoardingHouseRepository;
import com.qlpt.backend.repository.InvoiceRepository;
import com.qlpt.backend.repository.TaxDeclarationRepository;
import com.qlpt.backend.repository.TaxSettingRepository;
import com.qlpt.backend.service.TaxDeclarationService;
import com.qlpt.backend.utils.ExcelExportHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaxDeclarationServiceImpl implements TaxDeclarationService {

    private final TaxSettingRepository taxSettingRepository;
    private final TaxDeclarationRepository taxDeclarationRepository;
    private final InvoiceRepository invoiceRepository;
    private final BoardingHouseRepository boardingHouseRepository;

    public TaxDeclarationServiceImpl(TaxSettingRepository taxSettingRepository,
                                     TaxDeclarationRepository taxDeclarationRepository,
                                     InvoiceRepository invoiceRepository,
                                     BoardingHouseRepository boardingHouseRepository) {
        this.taxSettingRepository = taxSettingRepository;
        this.taxDeclarationRepository = taxDeclarationRepository;
        this.invoiceRepository = invoiceRepository;
        this.boardingHouseRepository = boardingHouseRepository;
    }

    @Override
    public TaxSetting getTaxSetting(User landlord) {
        return taxSettingRepository.findByLandlordId(landlord.getId())
                .orElseGet(() -> {
                    TaxSetting defaultSetting = TaxSetting.builder()
                            .landlord(landlord)
                            .annualThreshold(500000000.0) // 500 million VND
                            .vatRate(5.0) // 5% VAT
                            .pitRate(5.0) // 5% PIT
                            .build();
                    return taxSettingRepository.save(defaultSetting);
                });
    }

    @Override
    public TaxSetting updateTaxSetting(User landlord, TaxSettingRequest request) {
        TaxSetting setting = getTaxSetting(landlord);
        setting.setAnnualThreshold(request.getAnnualThreshold());
        setting.setVatRate(request.getVatRate());
        setting.setPitRate(request.getPitRate());
        return taxSettingRepository.save(setting);
    }

    @Override
    public List<TaxDeclarationResponse> getDeclarations(User landlord) {
        TaxSetting setting = getTaxSetting(landlord);
        List<TaxDeclaration> list = taxDeclarationRepository.findByLandlordOrderBySubmittedDateDesc(landlord);
        
        return list.stream().map(decl -> {
            double annualRevenue = getAnnualRevenue(landlord, decl.getYear());
            return TaxDeclarationResponse.fromEntity(decl, annualRevenue, setting.getAnnualThreshold());
        }).collect(Collectors.toList());
    }

    @Override
    public TaxDeclarationResponse calculateTax(User landlord, TaxDeclarationRequest request) {
        LocalDate[] dates = getPeriodDates(request.getYear(), request.getPeriodType(), request.getPeriodValue());
        LocalDate start = dates[0];
        LocalDate end = dates[1];

        List<Invoice> invoices = getFilteredInvoices(landlord, request.getBoardingHouseId(), start, end);
        double totalPaidInPeriod = invoices.stream()
                .filter(inv -> inv.getStatus() == com.qlpt.backend.enums.InvoiceStatus.PAID || 
                        inv.getStatus() == com.qlpt.backend.enums.InvoiceStatus.PARTIALLY_PAID)
                .mapToDouble(Invoice::getPaidAmount)
                .sum();

        double annualRevenue = getAnnualRevenue(landlord, request.getYear());
        TaxSetting setting = getTaxSetting(landlord);
        boolean isTaxable = annualRevenue > setting.getAnnualThreshold();

        double vatAmount = isTaxable ? totalPaidInPeriod * (setting.getVatRate() / 100.0) : 0.0;
        double pitAmount = isTaxable ? totalPaidInPeriod * (setting.getPitRate() / 100.0) : 0.0;
        double totalTaxAmount = vatAmount + pitAmount;

        String label = "";
        if ("MONTH".equals(request.getPeriodType())) {
            label = "Tháng " + request.getPeriodValue() + "/" + request.getYear();
        } else if ("QUARTER".equals(request.getPeriodType())) {
            label = "Quý " + convertToRoman(request.getPeriodValue()) + "/" + request.getYear();
        } else {
            label = "Năm " + request.getYear();
        }

        String bhName = "Tất cả dãy trọ";
        if (request.getBoardingHouseId() != null) {
            bhName = boardingHouseRepository.findById(request.getBoardingHouseId())
                    .map(BoardingHouse::getName)
                    .orElse("Dãy trọ");
        }

        return TaxDeclarationResponse.builder()
                .boardingHouseId(request.getBoardingHouseId())
                .boardingHouseName(bhName)
                .year(request.getYear())
                .periodType(request.getPeriodType())
                .periodValue(request.getPeriodValue())
                .periodLabel(label)
                .totalRevenue(totalPaidInPeriod)
                .vatAmount(vatAmount)
                .pitAmount(pitAmount)
                .totalTaxAmount(totalTaxAmount)
                .status("DRAFT")
                .isTaxable(isTaxable)
                .annualRevenueSoFar(annualRevenue)
                .annualThreshold(setting.getAnnualThreshold())
                .build();
    }

    @Override
    public TaxDeclarationResponse submitDeclaration(User landlord, TaxDeclarationRequest request) {
        // Check if already declared
        boolean exists = false;
        if (request.getBoardingHouseId() != null) {
            exists = taxDeclarationRepository.existsByLandlordAndYearAndPeriodTypeAndPeriodValueAndBoardingHouseId(
                    landlord, request.getYear(), request.getPeriodType(), request.getPeriodValue(), request.getBoardingHouseId());
        } else {
            exists = taxDeclarationRepository.existsByLandlordAndYearAndPeriodTypeAndPeriodValueAndBoardingHouseIsNull(
                    landlord, request.getYear(), request.getPeriodType(), request.getPeriodValue());
        }

        if (exists) {
            throw new RuntimeException("Tờ khai thuế cho thời gian và dãy trọ này đã được nộp trước đó.");
        }

        TaxDeclarationResponse preview = calculateTax(landlord, request);
        
        BoardingHouse boardingHouse = null;
        if (request.getBoardingHouseId() != null) {
            boardingHouse = boardingHouseRepository.findById(request.getBoardingHouseId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy dãy trọ"));
        }

        LocalDateTime now = LocalDateTime.now();
        String decNum = "MST-" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-" + (100000 + new java.util.Random().nextInt(900000));

        TaxDeclaration decl = TaxDeclaration.builder()
                .landlord(landlord)
                .boardingHouse(boardingHouse)
                .year(request.getYear())
                .periodType(request.getPeriodType())
                .periodValue(request.getPeriodValue())
                .totalRevenue(preview.getTotalRevenue())
                .vatAmount(preview.getVatAmount())
                .pitAmount(preview.getPitAmount())
                .totalTaxAmount(preview.getTotalTaxAmount())
                .declarationNumber(decNum)
                .status("SUBMITTED")
                .submittedDate(now)
                .taxAuthorityResponse("Cơ quan Thuế địa phương đã phê duyệt tự động tờ khai hợp lệ của bạn. Vui lòng thanh toán nghĩa vụ thuế trước hạn chót.")
                .build();

        TaxDeclaration saved = taxDeclarationRepository.save(decl);
        
        TaxSetting setting = getTaxSetting(landlord);
        double annualRevenue = getAnnualRevenue(landlord, request.getYear());
        return TaxDeclarationResponse.fromEntity(saved, annualRevenue, setting.getAnnualThreshold());
    }

    @Override
    public byte[] exportExcel(User landlord, UUID boardingHouseId, LocalDate start, LocalDate end) throws IOException {
        List<Invoice> invoices = getFilteredInvoices(landlord, boardingHouseId, start, end);
        String bhName = "Tất cả dãy trọ";
        if (boardingHouseId != null) {
            bhName = boardingHouseRepository.findById(boardingHouseId)
                    .map(BoardingHouse::getName)
                    .orElse("Dãy trọ");
        }
        TaxSetting setting = getTaxSetting(landlord);
        
        return ExcelExportHelper.exportRevenueToExcel(landlord, invoices, start, end, bhName, setting.getAnnualThreshold(), setting.getVatRate(), setting.getPitRate());
    }

    private double getAnnualRevenue(User landlord, int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        List<Invoice> annualInvoices = invoiceRepository.findByContractRoomBoardingHouseLandlordIdAndInvoiceDateBetween(
                landlord.getId(), yearStart, yearEnd);
        return annualInvoices.stream()
                .filter(inv -> inv.getStatus() == com.qlpt.backend.enums.InvoiceStatus.PAID || 
                        inv.getStatus() == com.qlpt.backend.enums.InvoiceStatus.PARTIALLY_PAID)
                .mapToDouble(Invoice::getPaidAmount)
                .sum();
    }

    private List<Invoice> getFilteredInvoices(User landlord, UUID boardingHouseId, LocalDate start, LocalDate end) {
        if (boardingHouseId != null) {
            return invoiceRepository.findByContractRoomBoardingHouseLandlordIdAndContractRoomBoardingHouseIdAndInvoiceDateBetween(
                    landlord.getId(), boardingHouseId, start, end);
        } else {
            return invoiceRepository.findByContractRoomBoardingHouseLandlordIdAndInvoiceDateBetween(
                    landlord.getId(), start, end);
        }
    }

    private LocalDate[] getPeriodDates(int year, String periodType, int periodValue) {
        LocalDate start;
        LocalDate end;
        if ("MONTH".equals(periodType)) {
            start = LocalDate.of(year, periodValue, 1);
            end = start.plusMonths(1).minusDays(1);
        } else if ("QUARTER".equals(periodType)) {
            start = LocalDate.of(year, (periodValue - 1) * 3 + 1, 1);
            end = start.plusMonths(3).minusDays(1);
        } else {
            start = LocalDate.of(year, 1, 1);
            end = LocalDate.of(year, 12, 31);
        }
        return new LocalDate[]{start, end};
    }

    private String convertToRoman(int number) {
        switch (number) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            default: return String.valueOf(number);
        }
    }
}
