package com.textile.erp.auth.service;

import com.textile.erp.auth.dto.LoginRequestDto;
import com.textile.erp.auth.dto.LoginResponseDto;
import com.textile.erp.auth.dto.LogoutRequestDto;
import com.textile.erp.auth.dto.LogoutResponseDto;
import com.textile.erp.auth.dto.RefreshTokenRequestDto;
import com.textile.erp.auth.dto.RefreshTokenResponseDto;
import com.textile.erp.auth.dto.UserAuthDto;
import com.textile.erp.auth.entity.RefreshToken;
import com.textile.erp.user.entity.RoleName;
import com.textile.erp.user.entity.Tenant;
import com.textile.erp.user.entity.TenantStatus;
import com.textile.erp.user.entity.User;
import com.textile.erp.user.entity.UserRole;
import com.textile.erp.user.entity.UserStatus;
import com.textile.erp.user.repository.TenantRepository;
import com.textile.erp.user.repository.UserRepository;
import com.textile.erp.user.repository.UserRoleRepository;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new BadCredentialsException("Email and password must be provided");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (normalizedEmail.isEmpty() || request.getPassword().isBlank()) {
            throw new BadCredentialsException("Email and password cannot be blank");
        }

        List<User> matchingUsers = userRepository.findByEmail(normalizedEmail);
        if (matchingUsers.isEmpty() && "admin".equalsIgnoreCase(normalizedEmail)) {
            matchingUsers = userRepository.findByEmail("admin@gmail.com");
        }
        if (matchingUsers.isEmpty()) {
            throw new BadCredentialsException("Invalid email or password");
        }

        // Match user by password hash
        User authenticatedUser = null;
        for (User user : matchingUsers) {
            if (user.getPasswordHash() != null && passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                authenticatedUser = user;
                break;
            }
        }

        if (authenticatedUser == null) {
            throw new BadCredentialsException("Invalid email or password");
        }

        // Validate user status
        if (authenticatedUser.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("User account is " + authenticatedUser.getStatus());
        }

        // Validate tenant status if user belongs to an organization
        if (authenticatedUser.getTenantId() != null) {
            Tenant tenant = tenantRepository.findById(authenticatedUser.getTenantId())
                    .orElseThrow(() -> new DisabledException("Assigned tenant organization was not found"));

            if (tenant.getStatus() != TenantStatus.ACTIVE) {
                throw new DisabledException("Tenant organization is " + tenant.getStatus());
            }
        }

        // Fetch user roles
        List<RoleName> roleNames = userRoleRepository.findByUserIdWithRole(authenticatedUser.getId()).stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        Set<RoleName> roleSet = Set.copyOf(roleNames);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(
                authenticatedUser.getId(),
                authenticatedUser.getTenantId(),
                authenticatedUser.getEmail(),
                roleSet
        );

        String refreshToken = refreshTokenService.createRefreshToken(authenticatedUser);

        UserAuthDto userDto = UserAuthDto.builder()
                .id(authenticatedUser.getId())
                .tenantId(authenticatedUser.getTenantId())
                .email(authenticatedUser.getEmail())
                .firstName(authenticatedUser.getFirstName())
                .lastName(authenticatedUser.getLastName())
                .roles(roleNames)
                .build();

        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .refreshToken(refreshToken)
                .user(userDto)
                .build();
    }

    @Override
    @Transactional
    public RefreshTokenResponseDto refreshToken(RefreshTokenRequestDto request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new BadCredentialsException("Refresh token is required");
        }

        RefreshToken currentToken = refreshTokenService.verifyRefreshToken(request.getRefreshToken());
        User user = currentToken.getUser();

        // Rotate refresh token by revoking the current one
        refreshTokenService.revokeToken(request.getRefreshToken());

        // Validate tenant if user is tenant-scoped
        if (user.getTenantId() != null) {
            Tenant tenant = tenantRepository.findById(user.getTenantId())
                    .orElseThrow(() -> new DisabledException("Tenant organization not found"));
            if (tenant.getStatus() != TenantStatus.ACTIVE) {
                throw new DisabledException("Tenant organization is " + tenant.getStatus());
            }
        }

        List<RoleName> roleNames = userRoleRepository.findByUserIdWithRole(user.getId()).stream()
                .map(ur -> ur.getRole().getName())
                .toList();

        Set<RoleName> roleSet = Set.copyOf(roleNames);

        String newAccessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getTenantId(),
                user.getEmail(),
                roleSet
        );

        String newRefreshToken = refreshTokenService.createRefreshToken(user);

        return RefreshTokenResponseDto.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationMs() / 1000)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Override
    @Transactional
    public LogoutResponseDto logout(LogoutRequestDto request) {
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            refreshTokenService.revokeToken(request.getRefreshToken());
        }

        return LogoutResponseDto.builder()
                .message("Logged out successfully")
                .build();
    }
}
