package com.textile.erp.user.repository;

import com.textile.erp.user.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);

    Optional<User> findByEmailAndTenantIdIsNull(String email);

    List<User> findByEmail(String email);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    boolean existsByEmailAndTenantIdIsNull(String email);

    List<User> findByTenantId(UUID tenantId);

    List<User> findByTenantIdIsNull();
}
