package com.qlpt.backend.repository;

import com.qlpt.backend.entity.BoardingHouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BoardingHouseRepository extends JpaRepository<BoardingHouse, UUID> {
    @EntityGraph(attributePaths = {"landlord"})
    Page<BoardingHouse> findByLandlordId(UUID landlordId, Pageable pageable);
}
