package com.textile.erp.user.repository;

import com.textile.erp.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.textile.erp.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.textile.erp.user.entity.RoleName;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);

    Optional<User> findByEmailAndTenantIdIsNull(String email);

    List<User> findByEmail(String email);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    boolean existsByEmailAndTenantIdIsNull(String email);

    List<User> findByTenantId(UUID tenantId);

    List<User> findByTenantIdIsNull();

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN UserRole ur ON ur.user = u
        LEFT JOIN ur.role r
        WHERE (:tenantId IS NULL OR u.tenantId = :tenantId)
          AND (:status IS NULL OR u.status = :status)
          AND (:role IS NULL OR r.name = :role)
          AND (
            :search IS NULL OR :search = '' OR
            LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
          )
    """)
    Page<User> searchUsers(
        @Param("tenantId") UUID tenantId,
        @Param("status") UserStatus status,
        @Param("role") RoleName role,
        @Param("search") String search,
        Pageable pageable
    );

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN UserRole ur ON ur.user = u
        LEFT JOIN ur.role r
        WHERE u.tenantId = :tenantId
          AND (:status IS NULL OR u.status = :status)
          AND (:role IS NULL OR r.name = :role)
          AND (
            :search IS NULL OR :search = '' OR
            LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
          )
    """)
    Page<User> searchUsersForTenant(
        @Param("tenantId") UUID tenantId,
        @Param("status") UserStatus status,
        @Param("role") RoleName role,
        @Param("search") String search,
        Pageable pageable
    );
}
