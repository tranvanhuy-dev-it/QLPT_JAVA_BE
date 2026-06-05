package com.qlpt.backend;

import com.qlpt.backend.dto.InvoiceCreateRequest;
import com.qlpt.backend.entity.*;
import com.qlpt.backend.repository.*;
import com.qlpt.backend.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceItemRepository invoiceItemRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ContractExtraFeeRepository contractExtraFeeRepository;

    @InjectMocks
    private InvoiceService invoiceService;

    private User landlord;
    private User tenant;
    private BoardingHouse boardingHouse;
    private Room room;
    private Contract contract;

    @BeforeEach
    public void setUp() {
        landlord = User.builder()
                .id(UUID.randomUUID())
                .username("landlord")
                .role(Role.LANDLORD)
                .build();

        tenant = User.builder()
                .id(UUID.randomUUID())
                .username("tenant")
                .role(Role.TENANT)
                .build();

        boardingHouse = BoardingHouse.builder()
                .id(UUID.randomUUID())
                .name("Nha tro Green House")
                .landlord(landlord)
                .defaultElectricityRate(3500)
                .defaultWaterRate(15000)
                .waterBillingType(WaterBillingType.BY_INDEX)
                .build();

        room = Room.builder()
                .id(UUID.randomUUID())
                .roomNumber("101")
                .basePrice(3000000)
                .currentElectricityIndex(100)
                .currentWaterIndex(20)
                .boardingHouse(boardingHouse)
                .status(RoomStatus.OCCUPIED)
                .build();

        contract = Contract.builder()
                .id(UUID.randomUUID())
                .room(room)
                .tenant(tenant)
                .startDate(LocalDate.of(2026, 5, 20))
                .contractedRoomPrice(3100000) // 3.1 triệu/tháng
                .billingMode(BillingMode.FIXED_DATE_OF_MONTH)
                .fixedBillingDay(5)
                .numberOfTenants(2)
                .status(ContractStatus.ACTIVE)
                .build();
    }

    @Test
    public void testCreateInvoice_ProratedCalculations() {
        // GIVEN
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setContractId(contract.getId());
        request.setInvoiceDate(LocalDate.of(2026, 6, 5));
        request.setBillingPeriodStart(LocalDate.of(2026, 5, 20));
        request.setBillingPeriodEnd(LocalDate.of(2026, 6, 5)); // 16 ngày ở thực tế
        request.setNewElectricityIndex(150); // Tiêu thụ 50 số điện
        request.setNewWaterIndex(25); // Tiêu thụ 5 khối nước

        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(contractExtraFeeRepository.findByContractId(contract.getId())).thenReturn(new ArrayList<>());
        
        // Mock save trả về chính đối tượng nhận vào
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Invoice invoice = invoiceService.createInvoice(request, landlord);

        // THEN
        assertNotNull(invoice);
        
        // Tính toán kiểm chứng:
        // Tiền phòng lẻ: (3,100,000 / 31 ngày của tháng 5) * 16 ngày = 1,600,000
        assertEquals(1600000.0, invoice.getRoomPrice(), "Tiền phòng lẻ chưa chính xác");
        
        // Tiền điện: (150 - 100) * 3500 = 175,000
        // Tiền nước: (25 - 20) * 15000 = 75,000
        // Tổng cộng = 1,600,000 + 175,000 + 75,000 = 1,850,000
        assertEquals(1850000.0, invoice.getTotalAmount(), "Tổng tiền hóa đơn chưa chính xác");

        // Xác nhận chỉ số đồng hồ của phòng được cập nhật
        assertEquals(150.0, room.getCurrentElectricityIndex());
        assertEquals(25.0, room.getCurrentWaterIndex());

        verify(roomRepository, times(1)).save(room);
        verify(invoiceRepository, times(2)).save(any(Invoice.class));
    }

    @Test
    public void testCreateInvoice_FullMonthFixedDate() {
        // GIVEN
        // Thiết lập bắt đầu chu kỳ từ ngày 5 đến ngày 5 tháng tiếp theo (Kỳ tính tiền trọn vẹn)
        contract.setStartDate(LocalDate.of(2026, 5, 5));
        
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setContractId(contract.getId());
        request.setInvoiceDate(LocalDate.of(2026, 6, 5));
        request.setBillingPeriodStart(LocalDate.of(2026, 5, 5));
        request.setBillingPeriodEnd(LocalDate.of(2026, 6, 5));
        request.setNewElectricityIndex(100); // Không đổi
        request.setNewWaterIndex(20); // Không đổi

        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(contractExtraFeeRepository.findByContractId(contract.getId())).thenReturn(new ArrayList<>());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Invoice invoice = invoiceService.createInvoice(request, landlord);

        // THEN
        assertNotNull(invoice);
        // Do tròn 1 tháng (từ ngày 5 đến ngày 5 tháng sau), tiền phòng tính đủ 3,100,000
        assertEquals(3100000.0, invoice.getRoomPrice(), "Tiền phòng trọn tháng chưa chính xác");
    }
}
