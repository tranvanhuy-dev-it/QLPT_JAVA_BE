package com.qlpt.backend.controller;

import com.qlpt.backend.config.CustomUserDetails;
import com.qlpt.backend.entity.UpgradeRequest;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.service.SubscriptionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/request")
    public ResponseEntity<UpgradeRequest> createRequest(
            @RequestBody Map<String, Integer> payload,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        int months = payload.getOrDefault("months", 3);
        User landlord = userDetails.getUser();
        UpgradeRequest request = subscriptionService.createUpgradeRequest(landlord, months);
        return ResponseEntity.ok(request);
    }

    @GetMapping("/my-requests")
    public ResponseEntity<List<UpgradeRequest>> getMyRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        List<UpgradeRequest> requests = subscriptionService.getMyRequests(landlord);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/active-status")
    public ResponseEntity<Map<String, Object>> getActiveStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        User landlord = userDetails.getUser();
        Map<String, Object> status = subscriptionService.getActiveStatus(landlord);
        return ResponseEntity.ok(status);
    }

    // ==========================================
    // ADMIN ONLY ENDPOINTS
    // ==========================================

    @GetMapping("/admin/requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UpgradeRequest>> getAllRequests(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<UpgradeRequest> requests = subscriptionService.getAllRequestsForAdmin(status, pageable);
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/admin/requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UpgradeRequest> approveRequest(@PathVariable UUID id) {
        UpgradeRequest approved = subscriptionService.approveRequest(id);
        return ResponseEntity.ok(approved);
    }

    @PostMapping("/admin/requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UpgradeRequest> rejectRequest(@PathVariable UUID id) {
        UpgradeRequest rejected = subscriptionService.rejectRequest(id);
        return ResponseEntity.ok(rejected);
    }

    @PostMapping("/admin/extend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> extendLandlord(
            @RequestBody Map<String, Object> payload) {
        UUID landlordId = UUID.fromString((String) payload.get("landlordId"));
        int months = ((Number) payload.get("months")).intValue();
        User landlord = subscriptionService.extendLandlordSubscriptionManually(landlordId, months);
        return ResponseEntity.ok(landlord);
    }
}
