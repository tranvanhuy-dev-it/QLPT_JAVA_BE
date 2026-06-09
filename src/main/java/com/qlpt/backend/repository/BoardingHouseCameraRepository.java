package com.qlpt.backend.repository;

import com.qlpt.backend.entity.BoardingHouseCamera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BoardingHouseCameraRepository extends JpaRepository<BoardingHouseCamera, UUID> {
    List<BoardingHouseCamera> findByBoardingHouseIdOrderByCreatedAtAsc(UUID boardingHouseId);
    List<BoardingHouseCamera> findByBoardingHouseLandlordIdOrderByCreatedAtAsc(UUID landlordId);

    @Query("SELECT bhc FROM BoardingHouseCamera bhc " +
           "JOIN bhc.boardingHouse bh " +
           "JOIN Room r ON r.boardingHouse = bh " +
           "JOIN Contract c ON c.room = r " +
           "WHERE c.tenant.id = :tenantId AND c.status = com.qlpt.backend.entity.ContractStatus.ACTIVE " +
           "ORDER BY bhc.createdAt ASC")
    List<BoardingHouseCamera> findTenantCameras(@Param("tenantId") UUID tenantId);
}
