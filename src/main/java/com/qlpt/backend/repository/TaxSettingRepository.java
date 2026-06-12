package com.qlpt.backend.repository;

import com.qlpt.backend.entity.TaxSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxSettingRepository extends JpaRepository<TaxSetting, UUID> {
    Optional<TaxSetting> findByLandlordId(UUID landlordId);
}
