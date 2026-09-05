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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

        User user = User.builder()
                .tenantId(request.getTenantId())
                .email(normalizedEmail)
                .passwordHash(request.getPassword()) // plain hash or null for OAuth
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName() != null ? request.getLastName().trim() : null)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(savedUser.getId(), role.getId()))
                .user(savedUser)
                .role(role)
                .build();

        userRoleRepository.save(userRole);

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
