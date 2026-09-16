package com.textile.erp.user.mapper;

import com.textile.erp.user.dto.UserResponse;
import com.textile.erp.user.dto.UserSummaryResponse;
import com.textile.erp.user.entity.RoleName;
import com.textile.erp.user.entity.User;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user, List<RoleName> roles) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .status(user.getStatus())
                .roles(roles != null ? roles : Collections.emptyList())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public UserSummaryResponse toSummary(User user, List<RoleName> roles) {
        if (user == null) {
            return null;
        }

        return UserSummaryResponse.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .status(user.getStatus())
                .roles(roles != null ? roles : Collections.emptyList())
                .build();
    }

    public UserSummaryResponse toSummaryResponse(User user, List<RoleName> roles) {
        return toSummary(user, roles);
    }
}
