package com.qlpt.backend.service;

import com.qlpt.backend.dto.InvoiceCreateRequest;
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
    private final ContractExtraFeeRepository contractExtraFeeRepository;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          InvoiceItemRepository invoiceItemRepository,
                          ContractRepository contractRepository,
                          RoomRepository roomRepository,
                          ContractExtraFeeRepository contractExtraFeeRepository) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceItemRepository = invoiceItemRepository;
        this.contractRepository = contractRepository;
        this.roomRepository = roomRepository;
        this.contractExtraFeeRepository = contractExtraFeeRepository;
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
        // 1. TÍNH TIỀN PHÒNG (Room Price Proration)
        // ==========================================
        double roomPrice = 0;
        if (request.getExcludeRoomPrice() == null || !request.getExcludeRoomPrice()) {
            if (contract.getBillingMode() == BillingMode.BY_RENTAL_DAYS) {
                // Tính theo tháng thuê mặc định trọn vẹn
                roomPrice = contract.getContractedRoomPrice();
            } else {
                // FIXED_DATE_OF_MONTH - Ngày cố định hàng tháng
                // Kiểm tra xem kỳ này có phải là 1 tháng trọn vẹn không
                boolean isFullMonth = start.getDayOfMonth() == contract.getFixedBillingDay() 
                        && end.getDayOfMonth() == contract.getFixedBillingDay() 
                        && ChronoUnit.DAYS.between(start, end) >= 28;

                if (isFullMonth) {
                    roomPrice = contract.getContractedRoomPrice();
                } else {
                    // Ở chưa đủ tháng -> Chia tiền phòng theo ngày thực tế ở
                    long stayedDays = ChronoUnit.DAYS.between(start, end);
                    int daysInStartMonth = start.lengthOfMonth();
                    double dailyRate = contract.getContractedRoomPrice() / daysInStartMonth;
                    roomPrice = Math.round(dailyRate * stayedDays); // Làm tròn tiền phòng
                }
            }
        }

        // ==========================================
        // 2. TÍNH TIỀN ĐIỆN
        // ==========================================
        double oldElec = room.getCurrentElectricityIndex();
        double newElec = request.getNewElectricityIndex();
        if (newElec < oldElec) {
            throw new RuntimeException("Chỉ số điện mới (" + newElec + ") không được nhỏ hơn chỉ số cũ (" + oldElec + ")");
        }
        double elecUsage = newElec - oldElec;
        double elecRate = boardingHouse.getDefaultElectricityRate();
        double elecTotal = Math.round(elecUsage * elecRate);

        // ==========================================
        // 3. TÍNH TIỀN NƯỚC
        // ==========================================
        double oldWater = 0;
        double newWater = 0;
        double waterRate = boardingHouse.getDefaultWaterRate();
        double waterTotal = 0;

        if (boardingHouse.getWaterBillingType() == WaterBillingType.BY_INDEX) {
            oldWater = room.getCurrentWaterIndex();
            newWater = request.getNewWaterIndex();
            if (newWater < oldWater) {
                throw new RuntimeException("Chỉ số nước mới (" + newWater + ") không được nhỏ hơn chỉ số cũ (" + oldWater + ")");
            }
            double waterUsage = newWater - oldWater;
            waterTotal = Math.round(waterUsage * waterRate);
        } else if (boardingHouse.getWaterBillingType() == WaterBillingType.FIXED_PER_PERSON) {
            // Tính theo số lượng người ở
            waterTotal = Math.round(contract.getNumberOfTenants() * waterRate);
        } else if (boardingHouse.getWaterBillingType() == WaterBillingType.FIXED_PER_ROOM) {
            // Tính cố định theo phòng
            waterTotal = waterRate;
        }

        // Khởi động hóa đơn
        double totalAmount = roomPrice + elecTotal + waterTotal;

        Invoice invoice = Invoice.builder()
                .contract(contract)
                .invoiceDate(request.getInvoiceDate())
                .billingPeriodStart(start)
                .billingPeriodEnd(end)
                .oldElectricityIndex(oldElec)
                .newElectricityIndex(newElec)
                .electricityRate(elecRate)
                .oldWaterIndex(oldWater)
                .newWaterIndex(newWater)
                .waterRate(waterRate)
                .roomPrice(roomPrice)
                .totalAmount(totalAmount) // Sẽ được cộng thêm phụ phí ở bước tiếp theo
                .paidAmount(0.0)
                .status(InvoiceStatus.PENDING)
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // ==========================================
        // 4. TÍNH CÁC PHỤ PHÍ DỊCH VỤ KHÁC (Extra Fees)
        // ==========================================
        List<InvoiceItem> invoiceItems = new ArrayList<>();
        double extraFeesTotal = 0;

        if (request.getExcludeExtraFees() == null || !request.getExcludeExtraFees()) {
            List<ContractExtraFee> contractExtraFees = contractExtraFeeRepository.findByContractId(contract.getId());
            for (ContractExtraFee cef : contractExtraFees) {
                ExtraFee ef = cef.getExtraFee();
                double quantity = 1;
                if (ef.getUnitType() == ExtraFeeUnitType.FIXED_PER_PERSON) {
                    quantity = contract.getNumberOfTenants();
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
        }

        // Cập nhật lại tổng tiền bao gồm phụ phí dịch vụ
        savedInvoice.setTotalAmount(totalAmount + extraFeesTotal);
        Invoice finalInvoice = invoiceRepository.save(savedInvoice);

        // ==========================================
        // 5. CẬP NHẬT CHỈ SỐ ĐỒNG HỒ ĐIỆN NƯỚC CỦA PHÒNG
        // ==========================================
        room.setCurrentElectricityIndex(newElec);
        if (boardingHouse.getWaterBillingType() == WaterBillingType.BY_INDEX) {
            room.setCurrentWaterIndex(newWater);
        }
        roomRepository.save(room);

        return finalInvoice;
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
        if (newPaid >= invoice.getTotalAmount()) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaymentDate(LocalDate.now());
        } else if (newPaid > 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        return invoiceRepository.save(invoice);
    }

    public Page<Invoice> getInvoicesByLandlord(User landlord, Pageable pageable) {
        return invoiceRepository.findByContractRoomBoardingHouseLandlordId(landlord.getId(), pageable);
    }

    public Page<Invoice> getInvoicesByTenant(User tenant, Pageable pageable) {
        return invoiceRepository.findByContractTenantId(tenant.getId(), pageable);
    }

    public Invoice getInvoiceById(UUID invoiceId, User user) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn"));

        boolean isLandlord = invoice.getContract().getRoom().getBoardingHouse().getLandlord().getId().equals(user.getId());
        boolean isTenant = invoice.getContract().getTenant().getId().equals(user.getId());

        if (!isLandlord && !isTenant && user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Bạn không có quyền xem hóa đơn này");
        }

        return invoice;
    }

    public List<InvoiceItem> getInvoiceItems(UUID invoiceId, User user) {
        // Tải hóa đơn để kiểm tra quyền truy cập trước
        getInvoiceById(invoiceId, user);
        return invoiceItemRepository.findByInvoiceId(invoiceId);
    }
}
