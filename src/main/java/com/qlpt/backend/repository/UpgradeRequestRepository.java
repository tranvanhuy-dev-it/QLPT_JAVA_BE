package com.qlpt.backend.repository;

import com.qlpt.backend.entity.UpgradeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UpgradeRequestRepository extends JpaRepository<UpgradeRequest, UUID> {
    List<UpgradeRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<UpgradeRequest> findByPaymentContent(String paymentContent);
    Page<UpgradeRequest> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    boolean existsByPaymentContent(String paymentContent);
}
