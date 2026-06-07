package com.qlpt.backend.repository;

import com.qlpt.backend.entity.ContractAddendumExtraFee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ContractAddendumExtraFeeRepository extends JpaRepository<ContractAddendumExtraFee, UUID> {
    List<ContractAddendumExtraFee> findByAddendumId(UUID addendumId);
}
