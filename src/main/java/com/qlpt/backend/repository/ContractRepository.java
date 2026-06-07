package com.qlpt.backend.repository;

import com.qlpt.backend.entity.Contract;
import com.qlpt.backend.entity.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {
    Optional<Contract> findByRoomIdAndStatus(UUID roomId, ContractStatus status);

    @EntityGraph(attributePaths = {"room", "tenant", "room.boardingHouse", "room.boardingHouse.landlord"})
    Page<Contract> findByTenantId(UUID tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"room", "tenant", "room.boardingHouse", "room.boardingHouse.landlord"})
    Page<Contract> findByRoomBoardingHouseLandlordId(UUID landlordId, Pageable pageable);
}
