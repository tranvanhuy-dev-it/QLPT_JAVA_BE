package com.qlpt.backend.repository;

import com.qlpt.backend.entity.ContractAddendum;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContractAddendumRepository extends JpaRepository<ContractAddendum, UUID> {
    List<ContractAddendum> findByContractIdOrderByStartDateDesc(UUID contractId);
    
    // Tìm phụ lục mới nhất có hiệu lực trước hoặc bằng một thời điểm (ví dụ ngày cuối kỳ tính tiền)
    Optional<ContractAddendum> findFirstByContractIdAndStartDateLessThanEqualOrderByStartDateDesc(UUID contractId, LocalDate date);
}
