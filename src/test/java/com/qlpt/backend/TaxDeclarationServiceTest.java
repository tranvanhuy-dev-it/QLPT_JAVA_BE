package com.qlpt.backend;

import com.qlpt.backend.dto.tax.TaxDeclarationRequest;
import com.qlpt.backend.dto.tax.TaxDeclarationResponse;
import com.qlpt.backend.dto.tax.TaxSettingRequest;
import com.qlpt.backend.entity.BoardingHouse;
import com.qlpt.backend.entity.Contract;
import com.qlpt.backend.entity.Invoice;
import com.qlpt.backend.entity.Room;
import com.qlpt.backend.entity.TaxSetting;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.enums.InvoiceStatus;
import com.qlpt.backend.enums.Role;
import com.qlpt.backend.repository.BoardingHouseRepository;
import com.qlpt.backend.repository.InvoiceRepository;
import com.qlpt.backend.repository.TaxDeclarationRepository;
import com.qlpt.backend.repository.TaxSettingRepository;
import com.qlpt.backend.repository.UserRepository;
import com.qlpt.backend.service.impl.TaxDeclarationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaxDeclarationServiceTest {

    @Mock
    private TaxSettingRepository taxSettingRepository;

    @Mock
    private TaxDeclarationRepository taxDeclarationRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private BoardingHouseRepository boardingHouseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TaxDeclarationServiceImpl taxDeclarationService;

    private User landlord;
    private TaxSetting taxSetting;
    private BoardingHouse boardingHouse;

    @BeforeEach
    public void setUp() {
        landlord = User.builder()
                .id(UUID.randomUUID())
                .username("landlord")
                .role(Role.LANDLORD)
                .build();

        taxSetting = TaxSetting.builder()
                .id(UUID.randomUUID())
                .landlord(landlord)
                .annualThreshold(500000000.0) // 500 million VND
                .vatRate(5.0)
                .pitRate(5.0)
                .build();

        boardingHouse = BoardingHouse.builder()
                .id(UUID.randomUUID())
                .name("Green Boarding House")
                .landlord(landlord)
                .build();
    }

    @Test
    public void testGetTaxSetting_DefaultValues() {
        when(userRepository.findById(landlord.getId())).thenReturn(Optional.of(landlord));
        when(taxSettingRepository.findByLandlordId(landlord.getId())).thenReturn(Optional.empty());
        when(taxSettingRepository.save(any(TaxSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        TaxSetting setting = taxDeclarationService.getTaxSetting(landlord);

        assertNotNull(setting);
        assertEquals(500000000.0, setting.getAnnualThreshold());
        assertEquals(5.0, setting.getVatRate());
        assertEquals(5.0, setting.getPitRate());
        assertEquals(landlord, setting.getLandlord());
    }

    @Test
    public void testUpdateTaxSetting() {
        when(userRepository.findById(landlord.getId())).thenReturn(Optional.of(landlord));
        when(taxSettingRepository.findByLandlordId(landlord.getId())).thenReturn(Optional.of(taxSetting));
        when(taxSettingRepository.save(any(TaxSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        TaxSettingRequest request = new TaxSettingRequest();
        request.setAnnualThreshold(300000000.0);
        request.setVatRate(4.0);
        request.setPitRate(4.0);

        TaxSetting updated = taxDeclarationService.updateTaxSetting(landlord, request);

        assertEquals(300000000.0, updated.getAnnualThreshold());
        assertEquals(4.0, updated.getVatRate());
        assertEquals(4.0, updated.getPitRate());
    }

    @Test
    public void testCalculateTax_BelowThreshold() {
        // GIVEN
        TaxDeclarationRequest request = new TaxDeclarationRequest();
        request.setYear(2026);
        request.setPeriodType("MONTH");
        request.setPeriodValue(6);
        request.setBoardingHouseId(null);

        // Mock userRepository and tax settings
        when(userRepository.findById(landlord.getId())).thenReturn(Optional.of(landlord));
        when(taxSettingRepository.findByLandlordId(landlord.getId())).thenReturn(Optional.of(taxSetting));

        // Mock invoices: total in June is 10M, total in year is 60M (below 500M)
        List<Invoice> juneInvoices = new ArrayList<>();
        Contract contract = Contract.builder().room(Room.builder().boardingHouse(boardingHouse).build()).build();
        juneInvoices.add(Invoice.builder().totalAmount(10000000.0).paidAmount(10000000.0).status(InvoiceStatus.PAID).contract(contract).build());

        when(invoiceRepository.findByContractRoomBoardingHouseLandlordIdAndInvoiceDateBetween(
                eq(landlord.getId()), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(juneInvoices); // 10M for June, 10M for the year total in mock

        // WHEN
        TaxDeclarationResponse response = taxDeclarationService.calculateTax(landlord, request);

        // THEN
        assertNotNull(response);
        assertEquals(10000000.0, response.getTotalRevenue());
        assertEquals(0.0, response.getVatAmount(), "Must be tax exempt");
        assertEquals(0.0, response.getPitAmount(), "Must be tax exempt");
        assertEquals(0.0, response.getTotalTaxAmount());
        assertFalse(response.isTaxable());
    }

    @Test
    public void testCalculateTax_AboveThreshold() {
        // GIVEN
        TaxDeclarationRequest request = new TaxDeclarationRequest();
        request.setYear(2026);
        request.setPeriodType("MONTH");
        request.setPeriodValue(6);
        request.setBoardingHouseId(null);

        when(userRepository.findById(landlord.getId())).thenReturn(Optional.of(landlord));
        when(taxSettingRepository.findByLandlordId(landlord.getId())).thenReturn(Optional.of(taxSetting));

        // Mock invoices: total in June is 50M.
        // Mock annual invoices: total in year is 600M (above 500M)
        List<Invoice> juneInvoices = new ArrayList<>();
        Contract contract = Contract.builder().room(Room.builder().boardingHouse(boardingHouse).build()).build();
        juneInvoices.add(Invoice.builder().totalAmount(50000000.0).paidAmount(50000000.0).status(InvoiceStatus.PAID).contract(contract).build());

        List<Invoice> annualInvoices = new ArrayList<>();
        annualInvoices.add(Invoice.builder().totalAmount(600000000.0).paidAmount(600000000.0).status(InvoiceStatus.PAID).contract(contract).build());

        // Stub find for period
        when(invoiceRepository.findByContractRoomBoardingHouseLandlordIdAndInvoiceDateBetween(
                eq(landlord.getId()), eq(LocalDate.of(2026, 6, 1)), eq(LocalDate.of(2026, 6, 30))))
                .thenReturn(juneInvoices);

        // Stub find for year
        when(invoiceRepository.findByContractRoomBoardingHouseLandlordIdAndInvoiceDateBetween(
                eq(landlord.getId()), eq(LocalDate.of(2026, 1, 1)), eq(LocalDate.of(2026, 12, 31))))
                .thenReturn(annualInvoices);

        // WHEN
        TaxDeclarationResponse response = taxDeclarationService.calculateTax(landlord, request);

        // THEN
        assertNotNull(response);
        assertEquals(50000000.0, response.getTotalRevenue());
        assertEquals(2500000.0, response.getVatAmount(), "5% VAT of 50M is 2.5M");
        assertEquals(2500000.0, response.getPitAmount(), "5% PIT of 50M is 2.5M");
        assertEquals(5000000.0, response.getTotalTaxAmount());
        assertTrue(response.isTaxable());
    }

    @Test
    public void testSubmitDeclaration_DuplicateThrows() {
        TaxDeclarationRequest request = new TaxDeclarationRequest();
        request.setYear(2026);
        request.setPeriodType("MONTH");
        request.setPeriodValue(6);
        request.setBoardingHouseId(null);

        when(userRepository.findById(landlord.getId())).thenReturn(Optional.of(landlord));
        when(taxDeclarationRepository.existsByLandlordAndYearAndPeriodTypeAndPeriodValueAndBoardingHouseIsNull(
                eq(landlord), eq(2026), eq("MONTH"), eq(6))).thenReturn(true);

        assertThrows(RuntimeException.class, () -> taxDeclarationService.submitDeclaration(landlord, request));
    }
}
