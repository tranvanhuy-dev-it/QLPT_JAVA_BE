package com.qlpt.backend.repository;

import com.qlpt.backend.entity.ContractExtraFee;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContractExtraFeeRepository extends JpaRepository<ContractExtraFee, UUID> {
    @EntityGraph(attributePaths = {"extraFee"})
    List<ContractExtraFee> findByContractId(UUID contractId);
    void deleteByContractId(UUID contractId);
}
