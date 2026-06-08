package com.qlpt.backend.repository;

import com.qlpt.backend.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {
    @EntityGraph(attributePaths = {"boardingHouse", "boardingHouse.landlord"})
    Optional<Room> findWithDetailsById(UUID id);
    long countByStatus(com.qlpt.backend.entity.RoomStatus status);

    @EntityGraph(attributePaths = {"boardingHouse", "boardingHouse.landlord"})
    Page<Room> findByBoardingHouseId(UUID boardingHouseId, Pageable pageable);

    @EntityGraph(attributePaths = {"boardingHouse", "boardingHouse.landlord"})
    Page<Room> findByBoardingHouseLandlordId(UUID landlordId, Pageable pageable);
}
