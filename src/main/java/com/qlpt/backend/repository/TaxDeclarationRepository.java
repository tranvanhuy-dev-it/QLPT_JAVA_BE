package com.qlpt.backend.repository;

import com.qlpt.backend.entity.TaxDeclaration;
import com.qlpt.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaxDeclarationRepository extends JpaRepository<TaxDeclaration, UUID> {
    List<TaxDeclaration> findByLandlordOrderBySubmittedDateDesc(User landlord);
    boolean existsByLandlordAndYearAndPeriodTypeAndPeriodValueAndBoardingHouseId(
            User landlord, int year, String periodType, int periodValue, UUID boardingHouseId);
    boolean existsByLandlordAndYearAndPeriodTypeAndPeriodValueAndBoardingHouseIsNull(
            User landlord, int year, String periodType, int periodValue);
}
