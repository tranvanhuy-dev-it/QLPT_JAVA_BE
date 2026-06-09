package com.qlpt.backend.service;

import com.qlpt.backend.entity.Contract;
import com.qlpt.backend.entity.ContractStatus;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.repository.ContractRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class PaymentReminderScheduler {

    private final ContractRepository contractRepository;
    private final NotificationService notificationService;

    public PaymentReminderScheduler(ContractRepository contractRepository,
                                    NotificationService notificationService) {
        this.contractRepository = contractRepository;
        this.notificationService = notificationService;
    }

    // Chạy vào lúc 8:00 sáng hàng ngày
    @Scheduled(cron = "0 0 8 * * *")
    public void sendPaymentReminders() {
        log.info("Bắt đầu quét hợp đồng để gửi nhắc nhở thanh toán...");
        
        List<Contract> activeContracts = contractRepository.findByStatus(ContractStatus.ACTIVE);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        int tomorrowDay = tomorrow.getDayOfMonth();
        
        for (Contract contract : activeContracts) {
            try {
                Integer billingDay = contract.getFixedBillingDay();
                if (billingDay == null || billingDay < 1 || billingDay > 31) {
                    billingDay = contract.getStartDate().getDayOfMonth();
                }
                
                boolean isReminderDay = false;
                if (tomorrowDay == billingDay) {
                    isReminderDay = true;
                } else {
                    // Xử lý các tháng ngắn ngày (ví dụ: ngày thanh toán là 31 nhưng tháng này chỉ có 30 ngày)
                    int maxTomorrowMonth = tomorrow.lengthOfMonth();
                    if (billingDay > maxTomorrowMonth && tomorrowDay == maxTomorrowMonth) {
                        isReminderDay = true;
                    }
                }
                
                if (isReminderDay) {
                    User landlord = contract.getRoom().getBoardingHouse().getLandlord();
                    User tenant = contract.getTenant();
                    String roomNumber = contract.getRoom().getRoomNumber();
                    
                    // Gửi thông báo cho Tenant
                    String tenantTitle = "Nhắc nhở đóng tiền phòng";
                    String tenantContent = String.format("Sắp đến ngày đóng tiền phòng %s (Hạn đóng: ngày %s/%s). Vui lòng chuẩn bị thanh toán cho chủ nhà.",
                            roomNumber, tomorrowDay, tomorrow.getMonthValue());
                    notificationService.createNotification(tenant, tenantTitle, tenantContent, "PAYMENT_REMINDER");
                    
                    // Gửi thông báo cho Landlord
                    String landlordTitle = "Hạn thu tiền phòng sắp tới";
                    String landlordContent = String.format("Phòng %s của người thuê %s sắp đến kỳ thanh toán vào ngày mai (%s/%s). Hãy chú ý kiểm tra và lập hóa đơn.",
                            roomNumber, tenant.getFullName(), tomorrowDay, tomorrow.getMonthValue());
                    notificationService.createNotification(landlord, landlordTitle, landlordContent, "PAYMENT_REMINDER");
                    
                    log.info("Đã gửi nhắc nhở thanh toán cho phòng: {}", roomNumber);
                }
            } catch (Exception e) {
                log.error("Lỗi khi gửi thông báo nhắc nhở cho hợp đồng " + contract.getId(), e);
            }
        }
        
        log.info("Hoàn thành quét nhắc nhở thanh toán.");
    }

    // Dọn dẹp thông báo cũ hơn 30 ngày, chạy vào lúc 1:00 sáng hàng ngày
    @Scheduled(cron = "0 0 1 * * *")
    public void cleanOldNotifications() {
        log.info("Bắt đầu dọn dẹp thông báo cũ hơn 30 ngày...");
        try {
            int deletedCount = notificationService.deleteNotificationsOlderThan(30);
            log.info("Đã dọn dẹp {} thông báo cũ hơn 30 ngày.", deletedCount);
        } catch (Exception e) {
            log.error("Lỗi khi dọn dẹp thông báo cũ hơn 30 ngày", e);
        }
    }
}
