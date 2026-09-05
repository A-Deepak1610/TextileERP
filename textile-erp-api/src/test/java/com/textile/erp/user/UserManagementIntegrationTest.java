package com.textile.erp.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.textile.erp.user.dto.TenantRequestDto;
import com.textile.erp.user.dto.TenantResponseDto;
import com.textile.erp.user.dto.UserCreateRequestDto;
import com.textile.erp.user.dto.UserResponseDto;
import com.textile.erp.user.entity.Role;
import com.textile.erp.user.entity.RoleName;
import com.textile.erp.user.entity.User;
import com.textile.erp.user.entity.UserStatus;
import com.textile.erp.user.repository.RoleRepository;
import com.textile.erp.user.repository.UserRepository;
import com.textile.erp.user.service.TenantService;
import com.textile.erp.user.service.UserService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class UserManagementIntegrationTest {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private UserService userService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Verify seeded roles exist from Flyway migration V1")
    void testSeededRolesExist() {
        Optional<Role> superAdmin = roleRepository.findByName(RoleName.SUPER_ADMIN);
        Optional<Role> tenantAdmin = roleRepository.findByName(RoleName.TENANT_ADMIN);
        Optional<Role> employee = roleRepository.findByName(RoleName.EMPLOYEE);

        assertThat(superAdmin).isPresent();
        assertThat(tenantAdmin).isPresent();
        assertThat(employee).isPresent();
    }

    @Test
    @DisplayName("Create Tenant with UUIDv7 ID and retrieve by slug")
    void testCreateTenant() {
        String uniqueSlug = "acme-textiles-" + System.currentTimeMillis();
        TenantRequestDto request = TenantRequestDto.builder()
                .name("Acme Textiles Corp")
                .slug(uniqueSlug)
                .build();

        TenantResponseDto created = tenantService.createTenant(request);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Acme Textiles Corp");
        assertThat(created.getSlug()).isEqualTo(uniqueSlug);
        assertThat(created.getStatus().name()).isEqualTo("ACTIVE");
        assertThat(created.getCreatedAt()).isNotNull();

        TenantResponseDto retrieved = tenantService.getTenantById(created.getId());
        assertThat(retrieved.getId()).isEqualTo(created.getId());
    }

    @Test
    @DisplayName("SUPER_ADMIN must have tenant_id = NULL and succeed")
    void testSuperAdminPlatformLevelSuccess() {
        String email = "platform.admin." + System.currentTimeMillis() + "@texforge.com";
        UserCreateRequestDto request = UserCreateRequestDto.builder()
                .tenantId(null)
                .email(email)
                .firstName("Platform")
                .lastName("SuperAdmin")
                .role(RoleName.SUPER_ADMIN)
                .build();

        UserResponseDto created = userService.createUser(request);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getTenantId()).isNull();
        assertThat(created.getEmail()).isEqualTo(email);
        assertThat(created.getRoles()).contains(RoleName.SUPER_ADMIN);
    }

    @Test
    @DisplayName("Attempting to create SUPER_ADMIN with a tenant_id must be rejected")
    void testSuperAdminWithTenantIdMustFail() {
        String uniqueSlug = "test-tenant-" + System.currentTimeMillis();
        TenantResponseDto tenant = tenantService.createTenant(TenantRequestDto.builder()
                .name("Test Tenant")
                .slug(uniqueSlug)
                .build());

        UserCreateRequestDto request = UserCreateRequestDto.builder()
                .tenantId(tenant.getId())
                .email("illegal.superadmin@texforge.com")
                .firstName("Illegal")
                .lastName("Admin")
                .role(RoleName.SUPER_ADMIN)
                .build();

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUPER_ADMIN is a platform-level user and must have tenantId = null");
    }

    @Test
    @DisplayName("Tenant users (TENANT_ADMIN, EMPLOYEE) require a valid tenant_id")
    void testTenantUsersRequireValidTenantId() {
        UserCreateRequestDto request = UserCreateRequestDto.builder()
                .tenantId(null)
                .email("no.tenant.admin@example.com")
                .firstName("Tenant")
                .lastName("Admin")
                .role(RoleName.TENANT_ADMIN)
                .build();

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be assigned to a valid tenantId");
    }

    @Test
    @DisplayName("Cross-tenant email reuse is allowed: same email in Tenant A and Tenant B")
    void testSameEmailAllowedAcrossDifferentTenants() {
        long ts = System.currentTimeMillis();
        TenantResponseDto tenantA = tenantService.createTenant(TenantRequestDto.builder()
                .name("Tenant A")
                .slug("tenant-a-" + ts)
                .build());

        TenantResponseDto tenantB = tenantService.createTenant(TenantRequestDto.builder()
                .name("Tenant B")
                .slug("tenant-b-" + ts)
                .build());

        String sharedEmail = "worker." + ts + "@sharedtextile.com";

        UserCreateRequestDto userA = UserCreateRequestDto.builder()
                .tenantId(tenantA.getId())
                .email(sharedEmail)
                .firstName("John")
                .lastName("Doe")
                .role(RoleName.EMPLOYEE)
                .build();

        UserCreateRequestDto userB = UserCreateRequestDto.builder()
                .tenantId(tenantB.getId())
                .email(sharedEmail)
                .firstName("John")
                .lastName("Doe")
                .role(RoleName.EMPLOYEE)
                .build();

        UserResponseDto createdA = userService.createUser(userA);
        UserResponseDto createdB = userService.createUser(userB);

        assertThat(createdA.getId()).isNotNull();
        assertThat(createdB.getId()).isNotNull();
        assertThat(createdA.getId()).isNotEqualTo(createdB.getId());
        assertThat(createdA.getTenantId()).isEqualTo(tenantA.getId());
        assertThat(createdB.getTenantId()).isEqualTo(tenantB.getId());
        assertThat(createdA.getEmail()).isEqualTo(createdB.getEmail());
    }

    @Test
    @DisplayName("Duplicate email within the same tenant must be rejected")
    void testDuplicateEmailWithinSameTenantRejected() {
        long ts = System.currentTimeMillis();
        TenantResponseDto tenant = tenantService.createTenant(TenantRequestDto.builder()
                .name("Single Tenant")
                .slug("single-tenant-" + ts)
                .build());

        String email = "duplicate." + ts + "@singletenant.com";

        UserCreateRequestDto firstUser = UserCreateRequestDto.builder()
                .tenantId(tenant.getId())
                .email(email)
                .firstName("First")
                .lastName("User")
                .role(RoleName.EMPLOYEE)
                .build();

        userService.createUser(firstUser);

        UserCreateRequestDto secondUser = UserCreateRequestDto.builder()
                .tenantId(tenant.getId())
                .email(email)
                .firstName("Second")
                .lastName("User")
                .role(RoleName.EMPLOYEE)
                .build();

        assertThatThrownBy(() -> userService.createUser(secondUser))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists in this tenant");
    }

    @Test
    @DisplayName("Duplicate platform email (tenant_id = NULL) must be rejected")
    void testDuplicatePlatformUserEmailRejected() {
        String email = "platform.duplicate." + System.currentTimeMillis() + "@texforge.com";

        UserCreateRequestDto firstAdmin = UserCreateRequestDto.builder()
                .tenantId(null)
                .email(email)
                .firstName("First")
                .lastName("Admin")
                .role(RoleName.SUPER_ADMIN)
                .build();

        userService.createUser(firstAdmin);

        UserCreateRequestDto secondAdmin = UserCreateRequestDto.builder()
                .tenantId(null)
                .email(email)
                .firstName("Second")
                .lastName("Admin")
                .role(RoleName.SUPER_ADMIN)
                .build();

        assertThatThrownBy(() -> userService.createUser(secondAdmin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Platform user with email");
    }

    @Test
    @DisplayName("Database partial unique index uq_platform_user_email rejects duplicate platform emails at DB level")
    void testDatabasePartialUniqueIndexPlatformUserEmail() {
        String email = "db.platform." + System.currentTimeMillis() + "@texforge.com";

        User user1 = User.builder()
                .tenantId(null)
                .email(email)
                .firstName("DB1")
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.saveAndFlush(user1);

        User user2 = User.builder()
                .tenantId(null)
                .email(email)
                .firstName("DB2")
                .status(UserStatus.ACTIVE)
                .build();

        assertThatThrownBy(() -> userRepository.saveAndFlush(user2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
