package com.textile.erp.auth.service;

import com.textile.erp.auth.entity.RefreshToken;
import com.textile.erp.auth.repository.RefreshTokenRepository;
import com.textile.erp.auth.util.TokenHashUtil;
import com.textile.erp.user.entity.User;
import com.textile.erp.user.entity.UserStatus;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Transactional
    public String createRefreshToken(User user) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        String tokenHash = TokenHashUtil.hashToken(rawToken);
        Instant expiresAt = Instant.now().plusMillis(refreshTokenExpirationMs);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.saveAndFlush(refreshToken);
        return rawToken;
    }

    @Transactional
    public RefreshToken verifyRefreshToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Refresh token cannot be blank");
        }

        String tokenHash = TokenHashUtil.hashToken(rawToken);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashWithUser(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid or unknown refresh token"));

        if (refreshToken.isRevoked()) {
            throw new BadCredentialsException("Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            throw new BadCredentialsException("Refresh token has expired");
        }

        User user = refreshToken.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new DisabledException("User account is " + user.getStatus());
        }

        return refreshToken;
    }

    @Transactional
    public void revokeToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String tokenHash = TokenHashUtil.hashToken(rawToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
            rt.setRevoked(true);
            rt.setRevokedAt(Instant.now());
            refreshTokenRepository.saveAndFlush(rt);
        });
    }

    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
