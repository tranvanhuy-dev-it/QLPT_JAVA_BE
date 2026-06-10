package com.qlpt.backend.service;

import com.qlpt.backend.entity.UpgradeRequest;
import com.qlpt.backend.entity.User;
import com.qlpt.backend.enums.Role;
import com.qlpt.backend.exception.ResourceNotFoundException;
import com.qlpt.backend.repository.UpgradeRequestRepository;
import com.qlpt.backend.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
