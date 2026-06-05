package com.qlpt.backend.repository;

import com.qlpt.backend.entity.ExtraFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExtraFeeRepository extends JpaRepository<ExtraFee, UUID> {
    List<ExtraFee> findByBoardingHouseId(UUID boardingHouseId);
}
