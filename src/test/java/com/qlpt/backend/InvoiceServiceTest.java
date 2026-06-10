package com.qlpt.backend;

import com.qlpt.backend.dto.invoice.InvoiceCreateRequest;
import com.qlpt.backend.dto.invoice.BulkBillingRoomStatus;
import com.qlpt.backend.entity.*;
import com.qlpt.backend.enums.*;
import com.qlpt.backend.repository.*;
import com.qlpt.backend.service.InvoiceService;
import com.qlpt.backend.service.NotificationService;
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
    private ContractAddendumRepository contractAddendumRepository;

    @Mock
    private ContractAddendumExtraFeeRepository contractAddendumExtraFeeRepository;

    @Mock
    private BoardingHouseRepository boardingHouseRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private InvoiceService invoiceService;

    private User landlord;
    private User tenant;
    private BoardingHouse boardingHouse;
    private Room room;
    private Contract contract;
    private ContractAddendum addendum;

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
                .numberOfTenants(2)
                .status(ContractStatus.ACTIVE)
                .build();

        // Phụ lục gốc - khởi tạo cùng contract
        addendum = ContractAddendum.builder()
                .id(UUID.randomUUID())
                .contract(contract)
                .startDate(LocalDate.of(2026, 5, 20))
                .roomPrice(3100000)
                .electricityRate(3500)
                .waterRate(15000)
                .waterBillingType(WaterBillingType.BY_INDEX)
                .numberOfTenants(2)
                .description("Phụ lục gốc")
                .extraFees(new ArrayList<>())
                .build();
    }

    @Test
    public void testCreateInvoice_ProratedCalculations() {
        // GIVEN - Kỳ thanh toán 16 ngày (20/05 -> 05/06)
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setContractId(contract.getId());
        request.setInvoiceDate(LocalDate.of(2026, 6, 5));
        request.setBillingPeriodStart(LocalDate.of(2026, 5, 20));
        request.setBillingPeriodEnd(LocalDate.of(2026, 6, 5)); // 16 ngày ở thực tế
        request.setNewElectricityIndex(150); // Tiêu thụ 50 số điện
        request.setNewWaterIndex(25); // Tiêu thụ 5 khối nước

        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(contractAddendumRepository.findFirstByContractIdAndStartDateLessThanEqualOrderByStartDateDesc(
                eq(contract.getId()), eq(LocalDate.of(2026, 6, 5))))
                .thenReturn(Optional.of(addendum));
        when(contractAddendumExtraFeeRepository.findByAddendumId(addendum.getId())).thenReturn(new ArrayList<>());

        // Mock save trả về chính đối tượng nhận vào
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Invoice invoice = invoiceService.createInvoice(request, landlord);

        // THEN
        assertNotNull(invoice);

        // Tính toán kiểm chứng:
        // Tiền phòng lẻ: Math.round((3,100,000 / 30) * 16) = 1,653,333
        assertEquals(1653333.0, invoice.getRoomPrice(), "Tiền phòng lẻ chưa chính xác");

        // Tiền điện: (150 - 100) * 3500 = 175,000
        // Tiền nước: (25 - 20) * 15000 = 75,000
        // Tổng cộng = 1,653,333 + 175,000 + 75,000 = 1,903,333
        assertEquals(1903333.0, invoice.getTotalAmount(), "Tổng tiền hóa đơn chưa chính xác");

        // Kiểm tra snapshot fields
        assertEquals(WaterBillingType.BY_INDEX, invoice.getWaterBillingType());
        assertEquals(2, invoice.getNumberOfTenants());
        assertEquals(3100000.0, invoice.getContractedRoomPrice());
        assertEquals(3500.0, invoice.getElectricityRate());
        assertEquals(15000.0, invoice.getWaterRate());

        // Xác nhận chỉ số đồng hồ của phòng được cập nhật
        assertEquals(150.0, room.getCurrentElectricityIndex());
        assertEquals(25.0, room.getCurrentWaterIndex());

        verify(roomRepository, times(1)).save(room);
        verify(invoiceRepository, times(2)).save(any(Invoice.class));
    }

    @Test
    public void testCreateInvoice_FullMonthBilling() {
        // GIVEN - Kỳ thanh toán trọn tháng (01/06 -> 30/06)
        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setContractId(contract.getId());
        request.setInvoiceDate(LocalDate.of(2026, 6, 30));
        request.setBillingPeriodStart(LocalDate.of(2026, 6, 1));
        request.setBillingPeriodEnd(LocalDate.of(2026, 6, 30));
        request.setNewElectricityIndex(100); // Không đổi
        request.setNewWaterIndex(20); // Không đổi

        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(contractAddendumRepository.findFirstByContractIdAndStartDateLessThanEqualOrderByStartDateDesc(
                eq(contract.getId()), eq(LocalDate.of(2026, 6, 30))))
                .thenReturn(Optional.of(addendum));
        when(contractAddendumExtraFeeRepository.findByAddendumId(addendum.getId())).thenReturn(new ArrayList<>());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Invoice invoice = invoiceService.createInvoice(request, landlord);

        // THEN
        assertNotNull(invoice);
        // Tính tỷ lệ theo 29 ngày: Math.round((3,100,000 / 30) * 29) = 2,996,667
        assertEquals(2996667.0, invoice.getRoomPrice(), "Tiền phòng lẻ theo ngày chưa chính xác");
    }

    @Test
    public void testCreateInvoice_WaterByPerson() {
        // GIVEN - Tính tiền nước theo đầu người
        ContractAddendum fixedWaterAddendum = ContractAddendum.builder()
                .id(UUID.randomUUID())
                .contract(contract)
                .startDate(LocalDate.of(2026, 5, 20))
                .roomPrice(3100000)
                .electricityRate(3500)
                .waterRate(100000) // 100,000 VND/người
                .waterBillingType(WaterBillingType.FIXED_PER_PERSON)
                .numberOfTenants(3)
                .description("Phụ lục chuyển tính nước theo đầu người")
                .extraFees(new ArrayList<>())
                .build();

        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setContractId(contract.getId());
        request.setInvoiceDate(LocalDate.of(2026, 6, 30));
        request.setBillingPeriodStart(LocalDate.of(2026, 6, 1));
        request.setBillingPeriodEnd(LocalDate.of(2026, 6, 30));
        request.setNewElectricityIndex(100); // Không đổi

        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(contractAddendumRepository.findFirstByContractIdAndStartDateLessThanEqualOrderByStartDateDesc(
                eq(contract.getId()), eq(LocalDate.of(2026, 6, 30))))
                .thenReturn(Optional.of(fixedWaterAddendum));
        when(contractAddendumExtraFeeRepository.findByAddendumId(fixedWaterAddendum.getId())).thenReturn(new ArrayList<>());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Invoice invoice = invoiceService.createInvoice(request, landlord);

        // THEN
        assertNotNull(invoice);
        // Tiền nước: 3 người * 100,000 = 300,000
        // Tiền phòng cho 29 ngày: Math.round((3,100,000 / 30) * 29) = 2,996,667
        double expectedWater = 300000.0;
        double expectedTotal = 2996667.0 + expectedWater; // phòng + nước (điện không đổi = 0)
        assertEquals(expectedTotal, invoice.getTotalAmount(), "Tổng tiền tính nước theo đầu người chưa chính xác");
        assertEquals(WaterBillingType.FIXED_PER_PERSON, invoice.getWaterBillingType());
        assertEquals(3, invoice.getNumberOfTenants());
    }

    @Test
    public void testCreateInvoice_UsesLatestAddendum() {
        // GIVEN - Phụ lục mới với giá phòng tăng
        ContractAddendum newAddendum = ContractAddendum.builder()
                .id(UUID.randomUUID())
                .contract(contract)
                .startDate(LocalDate.of(2026, 6, 1))
                .roomPrice(3500000) // Tăng từ 3.1M lên 3.5M
                .electricityRate(4000) // Tăng giá điện
                .waterRate(18000) // Tăng giá nước
                .waterBillingType(WaterBillingType.BY_INDEX)
                .numberOfTenants(2)
                .description("Phụ lục tăng giá")
                .extraFees(new ArrayList<>())
                .build();

        InvoiceCreateRequest request = new InvoiceCreateRequest();
        request.setContractId(contract.getId());
        request.setInvoiceDate(LocalDate.of(2026, 6, 30));
        request.setBillingPeriodStart(LocalDate.of(2026, 6, 1));
        request.setBillingPeriodEnd(LocalDate.of(2026, 6, 30));
        request.setNewElectricityIndex(120); // Tiêu thụ 20 số
        request.setNewWaterIndex(25); // Tiêu thụ 5 khối

        when(contractRepository.findById(contract.getId())).thenReturn(Optional.of(contract));
        when(contractAddendumRepository.findFirstByContractIdAndStartDateLessThanEqualOrderByStartDateDesc(
                eq(contract.getId()), eq(LocalDate.of(2026, 6, 30))))
                .thenReturn(Optional.of(newAddendum));
        when(contractAddendumExtraFeeRepository.findByAddendumId(newAddendum.getId())).thenReturn(new ArrayList<>());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        Invoice invoice = invoiceService.createInvoice(request, landlord);

        // THEN
        assertNotNull(invoice);
        // Kiểm tra dùng giá từ phụ lục mới
        assertEquals(3500000.0, invoice.getContractedRoomPrice(), "Snapshot đơn giá phòng phải từ phụ lục mới");
        // Tiền phòng cho 29 ngày: Math.round((3,500,000 / 30) * 29) = 3,383,333
        assertEquals(3383333.0, invoice.getRoomPrice(), "Tiền phòng phải dùng giá từ phụ lục mới");
        assertEquals(4000.0, invoice.getElectricityRate(), "Giá điện phải từ phụ lục mới");
        assertEquals(18000.0, invoice.getWaterRate(), "Giá nước phải từ phụ lục mới");

        // Tiền điện: 20 * 4000 = 80,000
        // Tiền nước: 5 * 18000 = 90,000
        // Tổng: 3,383,333 + 80,000 + 90,000 = 3,553,333
        assertEquals(3553333.0, invoice.getTotalAmount(), "Tổng tiền phải dùng giá từ phụ lục mới");
    }

    @Test
    public void testGetBillingStatusForBoardingHouse_FixedBillingDay() {
        // GIVEN
        UUID bhId = boardingHouse.getId();
        contract.setFixedBillingDay(5); // Ngày 5 hàng tháng
        
        List<Room> rooms = List.of(room);
        
        when(boardingHouseRepository.findById(bhId)).thenReturn(Optional.of(boardingHouse));
        when(roomRepository.findByBoardingHouseId(bhId)).thenReturn(rooms);
        when(contractRepository.findByRoomIdAndStatus(room.getId(), ContractStatus.ACTIVE)).thenReturn(Optional.of(contract));
        
        // Trực quan kỳ 1: chưa có hóa đơn cũ -> gợi ý từ ngày start (20/05/2026) đến ngày cố định tiếp theo (05/06/2026)
        when(invoiceRepository.findFirstByContractIdOrderByBillingPeriodEndDesc(contract.getId())).thenReturn(Optional.empty());
        
        LocalDate mockToday1 = LocalDate.of(2026, 5, 25);
        try (org.mockito.MockedStatic<LocalDate> mockedLocalDate = org.mockito.Mockito.mockStatic(LocalDate.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            // Giả lập hôm nay là 25/05/2026
            mockedLocalDate.when(LocalDate::now).thenReturn(mockToday1);
            
            // WHEN
            List<BulkBillingRoomStatus> statuses = invoiceService.getBillingStatusForBoardingHouse(bhId, landlord);
            
            // THEN
            assertNotNull(statuses);
            assertEquals(1, statuses.size());
            BulkBillingRoomStatus status = statuses.get(0);
            assertTrue(status.isHasActiveContract());
            assertEquals(LocalDate.of(2026, 5, 20), status.getNextBillingPeriodStart());
            assertEquals(LocalDate.of(2026, 6, 5), status.getNextBillingPeriodEnd());
            assertEquals(LocalDate.of(2026, 5, 20), status.getContractStartDate());
            assertEquals(5, status.getFixedBillingDay());
        }

        // Trực quan kỳ 2: có hóa đơn cũ kết thúc ngày 05/06/2026 -> gợi ý bắt đầu 06/06/2026 và kết thúc ngày 05/07/2026
        Invoice lastInvoice = Invoice.builder()
                .contract(contract)
                .invoiceDate(LocalDate.of(2026, 6, 5))
                .billingPeriodStart(LocalDate.of(2026, 5, 20))
                .billingPeriodEnd(LocalDate.of(2026, 6, 5))
                .build();
        when(invoiceRepository.findFirstByContractIdOrderByBillingPeriodEndDesc(contract.getId())).thenReturn(Optional.of(lastInvoice));

        LocalDate mockToday2 = LocalDate.of(2026, 6, 9);
        try (org.mockito.MockedStatic<LocalDate> mockedLocalDate2 = org.mockito.Mockito.mockStatic(LocalDate.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            // Giả lập hôm nay là 09/06/2026
            mockedLocalDate2.when(LocalDate::now).thenReturn(mockToday2);
            
            // WHEN
            List<BulkBillingRoomStatus> statuses2 = invoiceService.getBillingStatusForBoardingHouse(bhId, landlord);

            // THEN
            assertNotNull(statuses2);
            assertEquals(1, statuses2.size());
            BulkBillingRoomStatus status2 = statuses2.get(0);
            assertEquals(LocalDate.of(2026, 6, 6), status2.getNextBillingPeriodStart());
            assertEquals(LocalDate.of(2026, 7, 5), status2.getNextBillingPeriodEnd());
        }
    }
}
