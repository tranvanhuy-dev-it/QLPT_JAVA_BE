package com.qlpt.backend.repository;

import com.qlpt.backend.entity.BoardingHouseCamera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BoardingHouseCameraRepository extends JpaRepository<BoardingHouseCamera, UUID> {
    List<BoardingHouseCamera> findByBoardingHouseIdOrderByCreatedAtAsc(UUID boardingHouseId);
    List<BoardingHouseCamera> findByBoardingHouseLandlordIdOrderByCreatedAtAsc(UUID landlordId);
}
