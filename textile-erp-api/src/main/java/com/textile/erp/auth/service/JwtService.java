package com.textile.erp.auth.service;

import com.textile.erp.auth.security.CurrentUser;
import com.textile.erp.user.entity.RoleName;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secret;

    @Value("${jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate an access token.
     * Claims:
     * - sub: userId
     * - tenant_id: tenantId (null for SUPER_ADMIN)
     * - roles: list of role names
     */
    public String generateAccessToken(UUID userId, UUID tenantId, String email, Set<RoleName> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        List<String> roleNames = roles != null
                ? roles.stream().map(Enum::name).toList()
                : Collections.emptyList();

        boolean isSuperAdmin = roles != null && roles.contains(RoleName.SUPER_ADMIN);
        UUID effectiveTenantId = isSuperAdmin ? null : tenantId;

        return Jwts.builder()
                .subject(userId.toString())
                .claim("tenant_id", effectiveTenantId != null ? effectiveTenantId.toString() : null)
                .claim("roles", roleNames)
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public CurrentUser extractCurrentUser(String token) {
        Claims claims = extractAllClaims(token);
        UUID userId = UUID.fromString(claims.getSubject());

        String tenantIdStr = claims.get("tenant_id", String.class);
        UUID tenantId = (tenantIdStr != null && !tenantIdStr.isBlank())
                ? UUID.fromString(tenantIdStr)
                : null;

        String email = claims.get("email", String.class);

        @SuppressWarnings("unchecked")
        List<String> roleNames = claims.get("roles", List.class);
        Set<RoleName> roles = (roleNames != null)
                ? roleNames.stream()
                    .map(name -> {
                        try {
                            return RoleName.valueOf(name);
                        } catch (IllegalArgumentException ex) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet())
                : Collections.emptySet();

        return new CurrentUser(userId, tenantId, email, roles);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}
