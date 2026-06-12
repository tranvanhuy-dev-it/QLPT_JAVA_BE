package com.qlpt.backend.repository;

import com.qlpt.backend.entity.Contract;
import com.qlpt.backend.enums.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {
    Optional<Contract> findByRoomIdAndStatus(UUID roomId, ContractStatus status);

    @EntityGraph(attributePaths = {"room", "tenant", "room.boardingHouse", "room.boardingHouse.landlord", "addendums"})
    Optional<Contract> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"room", "tenant", "room.boardingHouse", "room.boardingHouse.landlord", "addendums"})
    Page<Contract> findByTenantId(UUID tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"room", "tenant", "room.boardingHouse", "room.boardingHouse.landlord", "addendums"})
    Page<Contract> findByTenantIdAndStatus(UUID tenantId, ContractStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"room", "tenant", "room.boardingHouse", "room.boardingHouse.landlord", "addendums"})
    Page<Contract> findByRoomBoardingHouseLandlordId(UUID landlordId, Pageable pageable);

    @EntityGraph(attributePaths = {"room", "tenant", "room.boardingHouse", "room.boardingHouse.landlord", "addendums"})
    Page<Contract> findByRoomBoardingHouseLandlordIdAndStatus(UUID landlordId, ContractStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"room", "tenant", "room.boardingHouse", "room.boardingHouse.landlord", "addendums"})
    Page<Contract> findByRoomId(UUID roomId, Pageable pageable);

    @EntityGraph(attributePaths = {"room", "tenant", "room.boardingHouse", "room.boardingHouse.landlord", "addendums"})
    Page<Contract> findByRoomIdAndStatus(UUID roomId, ContractStatus status, Pageable pageable);

    @Query("SELECT c.tenant.id FROM Contract c WHERE c.status = com.qlpt.backend.enums.ContractStatus.ACTIVE AND c.tenant.id IN :tenantIds")
    java.util.List<UUID> findTenantIdsWithActiveContracts(@Param("tenantIds") java.util.List<UUID> tenantIds);

    @EntityGraph(attributePaths = {"room", "tenant", "room.boardingHouse", "room.boardingHouse.landlord"})
    java.util.List<Contract> findByStatus(ContractStatus status);
}
