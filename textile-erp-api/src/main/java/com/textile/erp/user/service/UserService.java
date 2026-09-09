package com.textile.erp.user.service;

import com.textile.erp.user.dto.UserCreateRequestDto;
import com.textile.erp.user.dto.UserResponseDto;
import com.textile.erp.user.entity.RoleName;
import java.util.List;
import java.util.UUID;

import com.textile.erp.user.dto.UserResponse;

public interface UserService {

    UserResponseDto createUser(UserCreateRequestDto request);

    UserResponseDto getUserById(UUID userId);

    List<UserResponseDto> getUsersByTenantId(UUID tenantId);

    List<UserResponseDto> getPlatformUsers();

    void assignRoleToUser(UUID userId, RoleName roleName);

    UserResponse getCurrentUserProfile();
}
