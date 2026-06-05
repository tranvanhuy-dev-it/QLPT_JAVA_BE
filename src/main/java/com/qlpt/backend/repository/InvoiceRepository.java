package com.qlpt.backend.repository;

import com.qlpt.backend.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Page<Invoice> findByContractTenantId(UUID tenantId, Pageable pageable);
    Page<Invoice> findByContractRoomBoardingHouseLandlordId(UUID landlordId, Pageable pageable);
    List<Invoice> findByContractId(UUID contractId);
}
