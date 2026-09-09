package com.textile.erp.user.service;

import com.textile.erp.user.dto.UserCreateRequestDto;
import com.textile.erp.user.dto.UserResponseDto;
import com.textile.erp.user.entity.RoleName;
import java.util.List;
import java.util.UUID;

import com.textile.erp.user.dto.UserResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.textile.erp.user.dto.UserSummaryResponse;
import com.textile.erp.user.entity.UserStatus;

public interface UserService {

    UserResponseDto createUser(UserCreateRequestDto request);

    UserResponseDto getUserById(UUID userId);

    List<UserResponseDto> getUsersByTenantId(UUID tenantId);

    List<UserResponseDto> getPlatformUsers();

    void assignRoleToUser(UUID userId, RoleName roleName);

    UserResponse getCurrentUserProfile();

    UserResponse getUserByIdSecured(UUID userId);

    Page<UserSummaryResponse> listUsers(String search, RoleName role, UserStatus status, Pageable pageable);

    UserResponse provisionUser(com.textile.erp.user.dto.CreateUserRequest request);

    UserResponse updateUserProfile(UUID userId, com.textile.erp.user.dto.UpdateUserRequest request);

    UserResponse updateUserStatus(UUID userId, com.textile.erp.user.dto.UpdateUserStatusRequest request);

    UserResponse assignRole(UUID userId, com.textile.erp.user.dto.AssignRoleRequest request);

    UserResponse removeRole(UUID userId, Long roleId);
}
