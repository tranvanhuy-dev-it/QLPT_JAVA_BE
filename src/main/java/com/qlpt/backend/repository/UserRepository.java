package com.qlpt.backend.repository;

import com.qlpt.backend.enums.Role;
import com.qlpt.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    Page<User> findByRole(Role role, Pageable pageable);
    Page<User> findByRoleAndLandlordId(Role role, UUID landlordId, Pageable pageable);
    long countByRole(Role role);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.landlord.id = :landlordId " +
           "AND (:status IS NULL OR u.status = :status) " +
           "AND (:availableOnly IS NULL OR :availableOnly = false OR NOT EXISTS (" +
           "    SELECT c FROM Contract c WHERE c.tenant.id = u.id AND c.status = com.qlpt.backend.enums.ContractStatus.ACTIVE" +
           "))")
    Page<User> findTenants(@Param("role") Role role,
                           @Param("landlordId") UUID landlordId,
                           @Param("status") String status,
                           @Param("availableOnly") Boolean availableOnly,
                           Pageable pageable);
}
