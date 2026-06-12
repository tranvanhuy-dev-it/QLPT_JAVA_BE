package com.qlpt.backend.repository;

import com.qlpt.backend.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    @EntityGraph(attributePaths = {"contract", "contract.room", "contract.tenant", "contract.room.boardingHouse", "contract.room.boardingHouse.landlord", "contract.addendums"})
    Optional<Invoice> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"contract", "contract.room", "contract.tenant", "contract.room.boardingHouse", "contract.room.boardingHouse.landlord", "contract.addendums"})
    Page<Invoice> findByContractTenantId(UUID tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"contract", "contract.room", "contract.tenant", "contract.room.boardingHouse", "contract.room.boardingHouse.landlord", "contract.addendums"})
    Page<Invoice> findByContractRoomBoardingHouseLandlordId(UUID landlordId, Pageable pageable);

    List<Invoice> findByContractId(UUID contractId);

    Optional<Invoice> findFirstByContractIdOrderByBillingPeriodEndDesc(UUID contractId);

    @EntityGraph(attributePaths = {"contract", "contract.room", "contract.tenant", "contract.room.boardingHouse"})
    List<Invoice> findByContractRoomBoardingHouseLandlordIdAndInvoiceDateBetween(UUID landlordId, java.time.LocalDate start, java.time.LocalDate end);

    @EntityGraph(attributePaths = {"contract", "contract.room", "contract.tenant", "contract.room.boardingHouse"})
    List<Invoice> findByContractRoomBoardingHouseLandlordIdAndContractRoomBoardingHouseIdAndInvoiceDateBetween(UUID landlordId, UUID boardingHouseId, java.time.LocalDate start, java.time.LocalDate end);
}
