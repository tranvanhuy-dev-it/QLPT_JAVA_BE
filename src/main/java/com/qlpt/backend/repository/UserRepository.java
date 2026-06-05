package com.qlpt.backend.repository;

import com.qlpt.backend.entity.Role;
import com.qlpt.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    Page<User> findByRole(Role role, Pageable pageable);
    Page<User> findByRoleAndLandlordId(Role role, UUID landlordId, Pageable pageable);
}
