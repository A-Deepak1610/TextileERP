package com.textile.erp.user.service;

import com.textile.erp.user.dto.UserCreateRequestDto;
import com.textile.erp.user.dto.UserResponseDto;
import com.textile.erp.user.entity.Role;
import com.textile.erp.user.entity.RoleName;
import com.textile.erp.user.entity.User;
import com.textile.erp.user.entity.UserRole;
import com.textile.erp.user.entity.UserRoleId;
import com.textile.erp.user.entity.UserStatus;
import com.textile.erp.user.repository.RoleRepository;
import com.textile.erp.user.repository.TenantRepository;
import com.textile.erp.user.repository.UserRepository;
import com.textile.erp.user.repository.UserRoleRepository;
import com.textile.erp.user.security.UserSecurityValidator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import com.textile.erp.auth.security.CurrentUser;
import com.textile.erp.user.dto.UserResponse;
import com.textile.erp.user.dto.UserSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSecurityValidator userSecurityValidator;
    private final com.textile.erp.user.mapper.UserMapper userMapper;

    @Override
    @Transactional
    public UserResponseDto createUser(UserCreateRequestDto request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("User email cannot be blank");
        }
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be blank");
        }
        if (request.getRole() == null) {
            throw new IllegalArgumentException("User role must be specified");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        RoleName roleName = request.getRole();

        // Validate platform vs tenant hierarchy rules
        if (roleName == RoleName.SUPER_ADMIN) {
            if (request.getTenantId() != null) {
                throw new IllegalArgumentException("SUPER_ADMIN is a platform-level user and must have tenantId = null");
            }
            if (userRepository.existsByEmailAndTenantIdIsNull(normalizedEmail)) {
                throw new IllegalArgumentException("Platform user with email '" + normalizedEmail + "' already exists");
            }
        } else {
            // Tenant-scoped roles: TENANT_ADMIN, EMPLOYEE
            if (request.getTenantId() == null) {
                throw new IllegalArgumentException("Tenant users (" + roleName + ") must be assigned to a valid tenantId");
            }
            if (!tenantRepository.existsById(request.getTenantId())) {
                throw new NoSuchElementException("Tenant not found with ID: " + request.getTenantId());
            }
            if (userRepository.existsByTenantIdAndEmail(request.getTenantId(), normalizedEmail)) {
                throw new IllegalArgumentException("User with email '" + normalizedEmail + "' already exists in this tenant");
            }
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new NoSuchElementException("Role not found with name: " + roleName));

        String passwordHash = (request.getPassword() != null && !request.getPassword().isBlank())
                ? passwordEncoder.encode(request.getPassword())
                : null;

        User user = User.builder()
                .tenantId(request.getTenantId())
                .email(normalizedEmail)
                .passwordHash(passwordHash)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName() != null ? request.getLastName().trim() : null)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(savedUser.getId(), role.getId()))
                .user(savedUser)
                .role(role)
                .build();

        userRoleRepository.saveAndFlush(userRole);

        return mapToResponseDto(savedUser, List.of(roleName));
    }

    @Override
    public UserResponseDto getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));

        List<RoleName> roles = userRoleRepository.findByUserIdWithRole(userId).stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        return mapToResponseDto(user, roles);
    }

    @Override
    public List<UserResponseDto> getUsersByTenantId(UUID tenantId) {
        return userRepository.findByTenantId(tenantId).stream()
                .map(user -> {
                    List<RoleName> roles = userRoleRepository.findByUserIdWithRole(user.getId()).stream()
                            .map(ur -> ur.getRole().getName())
                            .toList();
                    return mapToResponseDto(user, roles);
                })
                .toList();
    }

    @Override
    public List<UserResponseDto> getPlatformUsers() {
        return userRepository.findByTenantIdIsNull().stream()
                .map(user -> {
                    List<RoleName> roles = userRoleRepository.findByUserIdWithRole(user.getId()).stream()
                            .map(ur -> ur.getRole().getName())
                            .toList();
                    return mapToResponseDto(user, roles);
                })
                .toList();
    }

    @Override
    @Transactional
    public void assignRoleToUser(UUID userId, RoleName roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new NoSuchElementException("Role not found with name: " + roleName));

        // Enforce hierarchy rule on role assignment
        if (roleName == RoleName.SUPER_ADMIN && user.getTenantId() != null) {
            throw new IllegalArgumentException("Cannot assign SUPER_ADMIN role to a tenant-scoped user");
        }
        if ((roleName == RoleName.TENANT_ADMIN || roleName == RoleName.EMPLOYEE) && user.isPlatformUser()) {
            throw new IllegalArgumentException("Cannot assign tenant-scoped role (" + roleName + ") to a platform-level user");
        }

        UserRoleId id = new UserRoleId(userId, role.getId());
        if (!userRoleRepository.existsById(id)) {
            UserRole userRole = UserRole.builder()
                    .id(id)
                    .user(user)
                    .role(role)
                    .build();
            userRoleRepository.save(userRole);
        }
    }

    @Override
    public com.textile.erp.user.dto.UserResponse getCurrentUserProfile() {
        com.textile.erp.auth.security.CurrentUser currentUser = userSecurityValidator.getAuthenticatedUser();
        User user = userRepository.findById(currentUser.getUserId())
                .orElseThrow(() -> new NoSuchElementException("Authenticated user not found with ID: " + currentUser.getUserId()));

        List<RoleName> roles = userRoleRepository.findByUserIdWithRole(user.getId()).stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        return userMapper.toResponse(user, roles);
    }

    @Override
    public UserResponse getUserByIdSecured(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));

        userSecurityValidator.validateCanAccessUser(user);

        List<RoleName> roles = userRoleRepository.findByUserIdWithRole(userId).stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        return userMapper.toResponse(user, roles);
    }

    @Override
    public Page<UserSummaryResponse> listUsers(String search, RoleName role, UserStatus status, Pageable pageable) {
        userSecurityValidator.validateCanListUsers();
        CurrentUser currentUser = userSecurityValidator.getAuthenticatedUser();

        Page<User> usersPage;
        if (currentUser.isSuperAdmin()) {
            usersPage = userRepository.searchUsers(null, status, role, search, pageable);
        } else {
            usersPage = userRepository.searchUsersForTenant(currentUser.getTenantId(), status, role, search, pageable);
        }

        return usersPage.map(user -> {
            List<RoleName> roles = userRoleRepository.findByUserIdWithRole(user.getId()).stream()
                    .map(ur -> ur.getRole().getName())
                    .toList();
            return userMapper.toSummaryResponse(user, roles);
        });
    }

    @Override
    @Transactional
    public UserResponse provisionUser(com.textile.erp.user.dto.CreateUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreateUserRequest cannot be null");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("User email cannot be blank");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be blank");
        }
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be blank");
        }
        if (request.getRole() == null) {
            throw new IllegalArgumentException("User role must be specified");
        }

        RoleName roleName = request.getRole();
        UUID effectiveTenantId = userSecurityValidator.resolveEffectiveTenantIdForCreation(roleName, request.getTenantId());

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (effectiveTenantId == null) {
            if (userRepository.existsByEmailAndTenantIdIsNull(normalizedEmail)) {
                throw new IllegalArgumentException("Platform user with email '" + normalizedEmail + "' already exists");
            }
        } else {
            if (!tenantRepository.existsById(effectiveTenantId)) {
                throw new NoSuchElementException("Tenant not found with ID: " + effectiveTenantId);
            }
            if (userRepository.existsByTenantIdAndEmail(effectiveTenantId, normalizedEmail)) {
                throw new IllegalArgumentException("User with email '" + normalizedEmail + "' already exists in this tenant");
            }
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new NoSuchElementException("Role not found with name: " + roleName));

        String passwordHash = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .tenantId(effectiveTenantId)
                .email(normalizedEmail)
                .passwordHash(passwordHash)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName() != null ? request.getLastName().trim() : null)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(savedUser.getId(), role.getId()))
                .user(savedUser)
                .role(role)
                .build();

        userRoleRepository.saveAndFlush(userRole);

        return userMapper.toResponse(savedUser, List.of(roleName));
    }

    @Override
    @Transactional
    public UserResponse updateUserProfile(UUID userId, com.textile.erp.user.dto.UpdateUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("UpdateUserRequest cannot be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));

        userSecurityValidator.validateCanModifyUser(user);

        if (request.getFirstName() != null) {
            if (request.getFirstName().trim().isEmpty()) {
                throw new IllegalArgumentException("First name cannot be empty");
            }
            user.setFirstName(request.getFirstName().trim());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim().isEmpty() ? null : request.getLastName().trim());
        }

        User updatedUser = userRepository.save(user);

        List<RoleName> roles = userRoleRepository.findByUserIdWithRole(userId).stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        return userMapper.toResponse(updatedUser, roles);
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(UUID userId, com.textile.erp.user.dto.UpdateUserStatusRequest request) {
        if (request == null || request.getStatus() == null) {
            throw new IllegalArgumentException("User status must not be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));

        userSecurityValidator.validateCanModifyUser(user);

        CurrentUser currentUser = userSecurityValidator.getAuthenticatedUser();
        if (java.util.Objects.equals(currentUser.getUserId(), user.getId()) && request.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Users cannot deactivate their own account");
        }

        user.setStatus(request.getStatus());
        User updatedUser = userRepository.save(user);

        List<RoleName> roles = userRoleRepository.findByUserIdWithRole(userId).stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        return userMapper.toResponse(updatedUser, roles);
    }

    @Override
    @Transactional
    public UserResponse assignRole(UUID userId, com.textile.erp.user.dto.AssignRoleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("AssignRoleRequest cannot be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));

        Role role;
        if (request.getRoleId() != null) {
            role = roleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new NoSuchElementException("Role not found with ID: " + request.getRoleId()));
        } else if (request.getRoleName() != null) {
            role = roleRepository.findByName(request.getRoleName())
                    .orElseThrow(() -> new NoSuchElementException("Role not found with name: " + request.getRoleName()));
        } else {
            throw new IllegalArgumentException("Either roleId or roleName must be provided");
        }

        userSecurityValidator.validateCanManageRoles(user, role.getName());

        if (role.getName() == RoleName.SUPER_ADMIN && user.getTenantId() != null) {
            throw new IllegalArgumentException("Cannot assign SUPER_ADMIN role to a tenant-scoped user");
        }
        if ((role.getName() == RoleName.TENANT_ADMIN || role.getName() == RoleName.EMPLOYEE) && user.isPlatformUser()) {
            throw new IllegalArgumentException("Cannot assign tenant-scoped role (" + role.getName() + ") to a platform user");
        }

        UserRoleId id = new UserRoleId(userId, role.getId());
        if (!userRoleRepository.existsById(id)) {
            UserRole userRole = UserRole.builder()
                    .id(id)
                    .user(user)
                    .role(role)
                    .build();
            userRoleRepository.saveAndFlush(userRole);
        }

        List<RoleName> roles = userRoleRepository.findByUserIdWithRole(userId).stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        return userMapper.toResponse(user, roles);
    }

    @Override
    @Transactional
    public UserResponse removeRole(UUID userId, Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("RoleId cannot be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found with ID: " + userId));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NoSuchElementException("Role not found with ID: " + roleId));

        userSecurityValidator.validateCanManageRoles(user, role.getName());

        long roleCount = userRoleRepository.countByIdUserId(userId);
        if (roleCount <= 1) {
            throw new IllegalStateException("Cannot remove the last remaining role from a user");
        }

        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
        userRoleRepository.flush();

        List<RoleName> roles = userRoleRepository.findByUserIdWithRole(userId).stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        return userMapper.toResponse(user, roles);
    }

    private UserResponseDto mapToResponseDto(User user, List<RoleName> roles) {
        return UserResponseDto.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .status(user.getStatus())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
