package com.qlpt.backend.repository;

import com.qlpt.backend.entity.User;
import com.qlpt.backend.entity.UserSession;
import com.qlpt.backend.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findByTokenAndActiveTrue(String token);
    boolean existsByTokenAndActiveTrue(String token);
    List<UserSession> findByUserOrderByLoginTimeDesc(User user);

    @Query("SELECT s FROM UserSession s JOIN s.user u WHERE " +
           "(:query IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:role IS NULL OR u.role = :role) " +
           "AND (:active IS NULL OR s.active = :active)")
    Page<UserSession> findSessions(@Param("query") String query,
                                   @Param("role") Role role,
                                   @Param("active") Boolean active,
                                   Pageable pageable);
}
