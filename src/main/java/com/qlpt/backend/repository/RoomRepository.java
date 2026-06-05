package com.qlpt.backend.repository;

import com.qlpt.backend.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {
    Page<Room> findByBoardingHouseId(UUID boardingHouseId, Pageable pageable);
    Page<Room> findByBoardingHouseLandlordId(UUID landlordId, Pageable pageable);
}
