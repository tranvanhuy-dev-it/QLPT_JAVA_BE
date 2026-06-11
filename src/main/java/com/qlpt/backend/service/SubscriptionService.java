package com.qlpt.backend.service;

import com.qlpt.backend.entity.UpgradeRequest;
import com.qlpt.backend.entity.User;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SubscriptionService {
    public UpgradeRequest createUpgradeRequest(User landlord, int months);

    public List<UpgradeRequest> getMyRequests(User landlord);

    public Page<UpgradeRequest> getPendingRequestsForAdmin(Pageable pageable);

    public Page<UpgradeRequest> getAllRequestsForAdmin(String status, Pageable pageable);

    public UpgradeRequest approveRequest(UUID requestId);

    public UpgradeRequest rejectRequest(UUID requestId);

    public User extendLandlordSubscriptionManually(UUID landlordId, int months);

    public Map<String, Object> getActiveStatus(User landlord);
}
