package com.qlpt.backend.service;

import com.qlpt.backend.entity.Role;
import com.qlpt.backend.entity.UpgradeRequest;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.exception.ResourceNotFoundException;
import com.qlpt.backend.repository.UpgradeRequestRepository;
import com.qlpt.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class SubscriptionService {

    private final UpgradeRequestRepository upgradeRequestRepository;
    private final UserRepository userRepository;

    public SubscriptionService(UpgradeRequestRepository upgradeRequestRepository, UserRepository userRepository) {
        this.upgradeRequestRepository = upgradeRequestRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public UpgradeRequest createUpgradeRequest(User landlord, int months) {
        if (landlord.getRole() != Role.LANDLORD) {
            throw new RuntimeException("Chỉ tài khoản chủ trọ mới có thể mua gói nâng cấp");
        }

        if (months != 3 && months != 6 && months != 12) {
            throw new RuntimeException("Gói đăng ký không hợp lệ! Vui lòng chọn gói 3, 6, hoặc 12 tháng.");
        }

        double amount = calculateAmount(months);
        String paymentContent = generateUniquePaymentContent(landlord.getUsername());

        UpgradeRequest request = UpgradeRequest.builder()
                .user(landlord)
                .months(months)
                .amount(amount)
                .status("PENDING")
                .paymentContent(paymentContent)
                .createdAt(LocalDateTime.now())
                .build();

        return upgradeRequestRepository.save(request);
    }

    public List<UpgradeRequest> getMyRequests(User landlord) {
        return upgradeRequestRepository.findByUserIdOrderByCreatedAtDesc(landlord.getId());
    }

    public Map<String, Object> getActiveStatus(User landlord) {
        Map<String, Object> statusMap = new HashMap<>();
        
        LocalDateTime regDateTime = landlord.getCreatedAt() != null ? landlord.getCreatedAt() : LocalDateTime.now().minusDays(50);
        LocalDate regDate = regDateTime.toLocalDate();
        LocalDate trialExpiredAt = regDate.plusDays(45);
        LocalDate subscriptionExpiredAt = landlord.getSubscriptionExpiredAt();
        
        LocalDate today = LocalDate.now();
        
        boolean isTrialActive = !today.isAfter(trialExpiredAt);
        boolean isSubscriptionActive = subscriptionExpiredAt != null && !today.isAfter(subscriptionExpiredAt);
        boolean isExpired = !isTrialActive && !isSubscriptionActive;

        long trialDaysLeft = Math.max(0, ChronoUnit.DAYS.between(today, trialExpiredAt));
        long subscriptionDaysLeft = subscriptionExpiredAt != null ? Math.max(0, ChronoUnit.DAYS.between(today, subscriptionExpiredAt)) : 0;

        statusMap.put("createdAt", regDateTime);
        statusMap.put("trialExpiredAt", trialExpiredAt);
        statusMap.put("subscriptionExpiredAt", subscriptionExpiredAt);
        statusMap.put("isTrialActive", isTrialActive);
        statusMap.put("isSubscriptionActive", isSubscriptionActive);
        statusMap.put("isExpired", isExpired);
        statusMap.put("trialDaysLeft", trialDaysLeft);
        statusMap.put("subscriptionDaysLeft", subscriptionDaysLeft);
        statusMap.put("role", landlord.getRole().name());
        
        return statusMap;
    }

    // ==========================================
    // ADMIN ACTIONS
    // ==========================================

    public Page<UpgradeRequest> getPendingRequestsForAdmin(Pageable pageable) {
        return upgradeRequestRepository.findByStatusOrderByCreatedAtDesc("PENDING", pageable);
    }

    public Page<UpgradeRequest> getAllRequestsForAdmin(String status, Pageable pageable) {
        if (status == null || status.trim().isEmpty()) {
            return upgradeRequestRepository.findAll(pageable);
        }
        return upgradeRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
    }

    @Transactional
    public UpgradeRequest approveRequest(UUID requestId) {
        UpgradeRequest request = upgradeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu nâng cấp"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Yêu cầu này đã được xử lý từ trước");
        }

        User landlord = request.getUser();
        int months = request.getMonths();

        LocalDate currentExpiry = landlord.getSubscriptionExpiredAt();
        LocalDate newExpiry;
        
        if (currentExpiry != null && currentExpiry.isAfter(LocalDate.now())) {
            newExpiry = currentExpiry.plusMonths(months);
        } else {
            newExpiry = LocalDate.now().plusMonths(months);
        }

        landlord.setSubscriptionExpiredAt(newExpiry);
        userRepository.save(landlord);

        request.setStatus("APPROVED");
        request.setProcessedAt(LocalDateTime.now());
        
        return upgradeRequestRepository.save(request);
    }

    @Transactional
    public UpgradeRequest rejectRequest(UUID requestId) {
        UpgradeRequest request = upgradeRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu nâng cấp"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Yêu cầu này đã được xử lý từ trước");
        }

        request.setStatus("REJECTED");
        request.setProcessedAt(LocalDateTime.now());
        
        return upgradeRequestRepository.save(request);
    }

    @Transactional
    public User extendLandlordSubscriptionManually(UUID landlordId, int months) {
        User landlord = userRepository.findById(landlordId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chủ trọ"));

        if (landlord.getRole() != Role.LANDLORD) {
            throw new RuntimeException("Chỉ có thể gia hạn tài khoản chủ trọ");
        }

        LocalDate currentExpiry = landlord.getSubscriptionExpiredAt();
        LocalDate newExpiry;

        if (currentExpiry != null && currentExpiry.isAfter(LocalDate.now())) {
            newExpiry = currentExpiry.plusMonths(months);
        } else {
            newExpiry = LocalDate.now().plusMonths(months);
        }

        landlord.setSubscriptionExpiredAt(newExpiry);
        return userRepository.save(landlord);
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

    private double calculateAmount(int months) {
        switch (months) {
            case 3:
                return 300000.0;
            case 6:
                return 550000.0;
            case 12:
                return 1000000.0;
            default:
                throw new IllegalArgumentException("Số tháng không hợp lệ");
        }
    }

    private String generateUniquePaymentContent(String username) {
        Random random = new Random();
        String content;
        do {
            int code = 1000 + random.nextInt(9000); // 4-digit code
            content = "GIAHAN " + username.toUpperCase() + " " + code;
        } while (upgradeRequestRepository.existsByPaymentContent(content));
        
        return content;
    }
}
