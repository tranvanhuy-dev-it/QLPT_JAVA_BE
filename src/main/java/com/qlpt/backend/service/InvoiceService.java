package com.qlpt.backend.service;

import com.qlpt.backend.dto.InvoiceCreateRequest;
import com.qlpt.backend.dto.BulkBillingRoomStatus;
import com.qlpt.backend.dto.BulkInvoiceCreateRequest;
import com.qlpt.backend.entity.*;
import com.qlpt.backend.exception.ResourceNotFoundException;
import com.qlpt.backend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final ContractAddendumRepository contractAddendumRepository;
    private final ContractAddendumExtraFeeRepository contractAddendumExtraFeeRepository;
    private final BoardingHouseRepository boardingHouseRepository;
    private final NotificationService notificationService;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          InvoiceItemRepository invoiceItemRepository,
                          ContractRepository contractRepository,
                          RoomRepository roomRepository,
                          ContractAddendumRepository contractAddendumRepository,
                          ContractAddendumExtraFeeRepository contractAddendumExtraFeeRepository,
                          BoardingHouseRepository boardingHouseRepository,
                          NotificationService notificationService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.contractRepository = contractRepository;
        this.roomRepository = roomRepository;
        this.contractAddendumRepository = contractAddendumRepository;
        this.contractAddendumExtraFeeRepository = contractAddendumExtraFeeRepository;
        this.boardingHouseRepository = boardingHouseRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Invoice createInvoice(InvoiceCreateRequest request, User landlord) {
        // Fetch contract
        Contract contract = contractRepository.findById(request.getContractId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hợp đồng"));

        Room room = contract.getRoom();
        BoardingHouse boardingHouse = room.getBoardingHouse();

        // Enforce landlord ownership check
        if (!boardingHouse.getLandlord().getId().equals(landlord.getId())) {
            throw new RuntimeException("Bạn không có quyền quản lý hóa đơn cho phòng trọ này");
        }

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new RuntimeException("Hợp đồng này không còn hoạt động, không thể tạo hóa đơn");
        }

        LocalDate start = request.getBillingPeriodStart();
        LocalDate end = request.getBillingPeriodEnd();

        if (end.isBefore(start) || end.isEqual(start)) {
            throw new RuntimeException("Ngày kết thúc kỳ thanh toán phải sau ngày bắt đầu");
        }

        // ==========================================
        // 0. TÌM PHỤ LỤC MỚI NHẤT CÓ HIỆU LỰC
        // ==========================================
        ContractAddendum latestAddendum = contractAddendumRepository
                .findFirstByContractIdAndStartDateLessThanEqualOrderByStartDateDesc(contract.getId(), end)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phụ lục hợp đồng có hiệu lực. Vui lòng kiểm tra lại hợp đồng."));

        // Lấy giá trị snapshot từ phụ lục mới nhất
        double snapshotRoomPrice = latestAddendum.getRoomPrice();
        double snapshotElecRate = latestAddendum.getElectricityRate();
        double snapshotWaterRate = latestAddendum.getWaterRate();
        WaterBillingType snapshotWaterBillingType = latestAddendum.getWaterBillingType();
        int snapshotNumberOfTenants = latestAddendum.getNumberOfTenants();

        // ==========================================
        // 1. TÍNH TIỀN PHÒNG (Room Price Proration)
        // ==========================================
        long stayedDays = ChronoUnit.DAYS.between(start, end);
        double dailyRate = snapshotRoomPrice / 30.0;
        double roomPrice = Math.round(dailyRate * stayedDays); // Làm tròn tiền phòng

        // ==========================================
        // 2. TÍNH TIỀN ĐIỆN
        // ==========================================
        double oldElec = room.getCurrentElectricityIndex();
        double newElec = request.getNewElectricityIndex();
        if (newElec < oldElec) {
            throw new RuntimeException("Chỉ số điện mới (" + newElec + ") không được nhỏ hơn chỉ số cũ (" + oldElec + ")");
        }
        double elecUsage = newElec - oldElec;
        double elecTotal = Math.round(elecUsage * snapshotElecRate);

        // ==========================================
        // 3. TÍNH TIỀN NƯỚC
        // ==========================================
        double oldWater = 0;
        double newWater = 0;
        double waterTotal = 0;

        if (snapshotWaterBillingType == WaterBillingType.BY_INDEX) {
            oldWater = room.getCurrentWaterIndex();
            newWater = request.getNewWaterIndex();
            if (newWater < oldWater) {
                throw new RuntimeException("Chỉ số nước mới (" + newWater + ") không được nhỏ hơn chỉ số cũ (" + oldWater + ")");
            }
            double waterUsage = newWater - oldWater;
            waterTotal = Math.round(waterUsage * snapshotWaterRate);
        } else if (snapshotWaterBillingType == WaterBillingType.FIXED_PER_PERSON) {
            // Tính theo số lượng người ở (từ phụ lục mới nhất)
            waterTotal = Math.round(snapshotNumberOfTenants * snapshotWaterRate);
        }

        // Khởi tạo hóa đơn với snapshot fields
        double discount = request.getDiscount() != null ? request.getDiscount() : 0.0;
        double totalAmount = roomPrice + elecTotal + waterTotal - discount;

        Invoice invoice = Invoice.builder()
                .contract(contract)
                .invoiceDate(request.getInvoiceDate())
                .billingPeriodStart(start)
                .billingPeriodEnd(end)
                .oldElectricityIndex(oldElec)
                .newElectricityIndex(newElec)
                .electricityRate(snapshotElecRate)
                .oldWaterIndex(oldWater)
                .newWaterIndex(newWater)
                .waterRate(snapshotWaterRate)
                .roomPrice(roomPrice)
                .waterBillingType(snapshotWaterBillingType)
                .numberOfTenants(snapshotNumberOfTenants)
                .contractedRoomPrice(snapshotRoomPrice)
                .discount(discount)
                .totalAmount(totalAmount) // Sẽ được cộng thêm phụ phí ở bước tiếp theo
                .paidAmount(0.0)
                .status(InvoiceStatus.PENDING)
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // ==========================================
        // 4. TÍNH CÁC PHỤ PHÍ DỊCH VỤ KHÁC (Extra Fees từ Addendum)
        // ==========================================
        List<InvoiceItem> invoiceItems = new ArrayList<>();
        double extraFeesTotal = 0;

        List<ContractAddendumExtraFee> addendumExtraFees = contractAddendumExtraFeeRepository.findByAddendumId(latestAddendum.getId());
        for (ContractAddendumExtraFee cef : addendumExtraFees) {
            ExtraFee ef = cef.getExtraFee();
            double quantity = 1;
            if (ef.getUnitType() == ExtraFeeUnitType.FIXED_PER_PERSON) {
                quantity = snapshotNumberOfTenants;
            }

            double subtotal = Math.round(cef.getCustomPrice() * quantity);
            extraFeesTotal += subtotal;

            InvoiceItem item = InvoiceItem.builder()
                    .invoice(savedInvoice)
                    .name(ef.getName())
                    .price(cef.getCustomPrice())
                    .quantity(quantity)
                    .subtotal(subtotal)
                    .build();

            invoiceItems.add(item);
        }

        // Lưu chi tiết các phụ phí dịch vụ
        if (!invoiceItems.isEmpty()) {
            invoiceItemRepository.saveAll(invoiceItems);
        }

        // Cập nhật lại tổng tiền bao gồm phụ phí dịch vụ
        savedInvoice.setTotalAmount(totalAmount + extraFeesTotal);
        Invoice finalInvoice = invoiceRepository.save(savedInvoice);

        // ==========================================
        // 5. CẬP NHẬT CHỈ SỐ ĐỒNG HỒ ĐIỆN NƯỚC CỦA PHÒNG
        // ==========================================
        room.setCurrentElectricityIndex(newElec);
        if (snapshotWaterBillingType == WaterBillingType.BY_INDEX) {
            room.setCurrentWaterIndex(newWater);
        }
        roomRepository.save(room);

        Invoice resultInvoice = invoiceRepository.findWithDetailsById(finalInvoice.getId()).orElse(finalInvoice);

        // Gửi thông báo đến người thuê
        try {
            String title = "Hóa đơn mới được tạo";
            String content = String.format("Hóa đơn phòng %s kỳ từ %s đến %s với số tiền %s đã được tạo. Vui lòng thanh toán sớm.",
                    room.getRoomNumber(),
                    start,
                    end,
                    String.format("%,.0f VNĐ", resultInvoice.getTotalAmount()));
            notificationService.createNotification(contract.getTenant(), title, content, "INVOICE_NEW");
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi thông báo hóa đơn mới: " + e.getMessage());
        }

        return resultInvoice;
    }

    @Transactional
    public Invoice updatePaymentStatus(UUID invoiceId, double paidAmount, User landlord) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn"));

        if (!invoice.getContract().getRoom().getBoardingHouse().getLandlord().getId().equals(landlord.getId())) {
            throw new RuntimeException("Bạn không có quyền cập nhật trạng thái thanh toán hóa đơn này");
        }

        double newPaid = invoice.getPaidAmount() + paidAmount;
        if (newPaid > invoice.getTotalAmount()) {
            throw new RuntimeException("Số tiền thanh toán vượt quá số tiền còn nợ của hóa đơn");
        }

        invoice.setPaidAmount(newPaid);
        boolean isFullyPaid = false;
        if (newPaid >= invoice.getTotalAmount()) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaymentDate(LocalDate.now());
            isFullyPaid = true;
        } else if (newPaid > 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        Invoice saved = invoiceRepository.save(invoice);
        Invoice result = invoiceRepository.findWithDetailsById(saved.getId()).orElse(saved);

        if (isFullyPaid) {
            try {
                String title = "Xác nhận thanh toán hóa đơn";
                String content = String.format("Hóa đơn phòng %s kỳ từ %s đến %s đã được xác nhận thanh toán thành công.",
                        result.getContract().getRoom().getRoomNumber(),
                        result.getBillingPeriodStart(),
                        result.getBillingPeriodEnd());
                notificationService.createNotification(result.getContract().getTenant(), title, content, "PAYMENT_CONFIRMED");
            } catch (Exception e) {
                System.err.println("Lỗi khi gửi thông báo xác nhận thanh toán: " + e.getMessage());
            }
        }

        return result;
    }

    @Transactional(readOnly = true)
    public Page<Invoice> getInvoicesByLandlord(User landlord, Pageable pageable) {
        return invoiceRepository.findByContractRoomBoardingHouseLandlordId(landlord.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Invoice> getInvoicesByTenant(User tenant, Pageable pageable) {
        return invoiceRepository.findByContractTenantId(tenant.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Invoice getInvoiceById(UUID invoiceId, User user) {
        Invoice invoice = invoiceRepository.findWithDetailsById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn"));

        boolean isLandlord = invoice.getContract().getRoom().getBoardingHouse().getLandlord().getId().equals(user.getId());
        boolean isTenant = invoice.getContract().getTenant().getId().equals(user.getId());

        if (!isLandlord && !isTenant && user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Bạn không có quyền xem hóa đơn này");
        }

        return invoice;
    }

    @Transactional(readOnly = true)
    public List<InvoiceItem> getInvoiceItems(UUID invoiceId, User user) {
        // Tải hóa đơn để kiểm tra quyền truy cập trước
        getInvoiceById(invoiceId, user);
        return invoiceItemRepository.findByInvoiceId(invoiceId);
    }

    @Transactional(readOnly = true)
    public List<BulkBillingRoomStatus> getBillingStatusForBoardingHouse(UUID bhId, User landlord) {
        BoardingHouse boardingHouse = boardingHouseRepository.findById(bhId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dãy trọ"));

        if (!boardingHouse.getLandlord().getId().equals(landlord.getId())) {
            throw new RuntimeException("Bạn không có quyền quản lý dãy trọ này");
        }

        List<Room> rooms = roomRepository.findByBoardingHouseId(bhId);
        List<BulkBillingRoomStatus> result = new ArrayList<>();

        for (Room room : rooms) {
            java.util.Optional<Contract> activeContractOpt = contractRepository.findByRoomIdAndStatus(room.getId(), ContractStatus.ACTIVE);
            
            BulkBillingRoomStatus.BulkBillingRoomStatusBuilder builder = BulkBillingRoomStatus.builder()
                    .roomId(room.getId())
                    .roomNumber(room.getRoomNumber())
                    .roomStatus(room.getStatus())
                    .currentElectricityIndex(room.getCurrentElectricityIndex())
                    .currentWaterIndex(room.getCurrentWaterIndex())
                    .defaultElectricityRate(boardingHouse.getDefaultElectricityRate())
                    .defaultWaterRate(boardingHouse.getDefaultWaterRate())
                    .waterBillingType(boardingHouse.getWaterBillingType());

            if (activeContractOpt.isPresent()) {
                Contract contract = activeContractOpt.get();
                builder.hasActiveContract(true)
                        .contractId(contract.getId())
                        .tenantName(contract.getTenant().getFullName())
                        .contractStartDate(contract.getStartDate())
                        .fixedBillingDay(contract.getFixedBillingDay());

                // Tìm hóa đơn cuối cùng để biết ngày bắt đầu
                java.util.Optional<Invoice> lastInvoiceOpt = invoiceRepository.findFirstByContractIdOrderByBillingPeriodEndDesc(contract.getId());
                LocalDate nextStart;
                if (lastInvoiceOpt.isPresent()) {
                    nextStart = lastInvoiceOpt.get().getInvoiceDate().plusDays(1);
                } else {
                    nextStart = contract.getStartDate();
                }
                builder.nextBillingPeriodStart(nextStart);

                // Tính toán gợi ý ngày kết thúc (nextBillingPeriodEnd) dựa trên ngày cố định
                LocalDate nextEnd;
                Integer billingDay = contract.getFixedBillingDay();
                if (billingDay != null && billingDay >= 1 && billingDay <= 31) {
                    // Nếu thanh toán theo ngày cố định
                    LocalDate candidateEnd = nextStart;
                    int targetYear = nextStart.getYear();
                    int targetMonth = nextStart.getMonthValue();
                    
                    try {
                        candidateEnd = LocalDate.of(targetYear, targetMonth, Math.min(billingDay, LocalDate.of(targetYear, targetMonth, 1).lengthOfMonth()));
                    } catch (Exception e) {}

                    if (candidateEnd.isBefore(nextStart) || candidateEnd.isEqual(nextStart)) {
                        targetMonth++;
                        if (targetMonth > 12) {
                            targetMonth = 1;
                            targetYear++;
                        }
                        try {
                            candidateEnd = LocalDate.of(targetYear, targetMonth, Math.min(billingDay, LocalDate.of(targetYear, targetMonth, 1).lengthOfMonth()));
                        } catch (Exception e) {}
                    }
                    nextEnd = candidateEnd;
                } else {
                    // Mặc định tính từ ngày chuyển vào -> gợi ý tròn 1 tháng (trừ 1 ngày)
                    nextEnd = nextStart.plusMonths(1).minusDays(1);
                }
                builder.nextBillingPeriodEnd(nextEnd);
            } else {
                builder.hasActiveContract(false);
            }

            result.add(builder.build());
        }

        return result;
    }

    @Transactional
    public void createBulkInvoices(BulkInvoiceCreateRequest request, User landlord) {
        if (request.getReadings() == null || request.getReadings().isEmpty()) {
            return;
        }

        for (BulkInvoiceCreateRequest.RoomReading reading : request.getReadings()) {
            if (reading.getContractId() == null) {
                continue;
            }

            InvoiceCreateRequest createReq = new InvoiceCreateRequest();
            createReq.setContractId(reading.getContractId());
            createReq.setInvoiceDate(request.getInvoiceDate());
            createReq.setBillingPeriodStart(reading.getBillingPeriodStart());
            createReq.setBillingPeriodEnd(reading.getBillingPeriodEnd());
            createReq.setNewElectricityIndex(reading.getNewElectricityIndex());
            createReq.setNewWaterIndex(reading.getNewWaterIndex());
            createReq.setDiscount(reading.getDiscount());

            createInvoice(createReq, landlord);
        }
    }
}
