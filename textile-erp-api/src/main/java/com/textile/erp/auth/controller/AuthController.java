package com.textile.erp.auth.controller;

import com.textile.erp.auth.dto.LoginRequestDto;
import com.textile.erp.auth.dto.LoginResponseDto;
import com.textile.erp.auth.dto.LogoutRequestDto;
import com.textile.erp.auth.dto.LogoutResponseDto;
import com.textile.erp.auth.dto.RefreshTokenRequestDto;
import com.textile.erp.auth.dto.RefreshTokenResponseDto;
import com.textile.erp.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication, token refresh, and session revocation")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
            summary = "User Login",
            description = "Authenticates user credentials using email and password. Upon successful validation, issues a Bearer JWT access token (for API authorization) and a refresh token (for rotating expired access tokens)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated; returns tokens and user profile"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or missing credentials"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password / authentication failed")
    })
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh Access Token",
            description = "Generates a new JWT access token using a valid, unexpired refresh token without requiring the user to re-enter their credentials."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully refreshed access token"),
            @ApiResponse(responseCode = "400", description = "Missing or malformed refresh token"),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or revoked refresh token")
    })
    public ResponseEntity<RefreshTokenResponseDto> refresh(@RequestBody RefreshTokenRequestDto request) {
        RefreshTokenResponseDto response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(
            summary = "User Logout",
            description = "Revokes the active refresh token session, invalidating subsequent token refresh attempts."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully logged out and session revoked")
    })
    public ResponseEntity<LogoutResponseDto> logout(@RequestBody(required = false) LogoutRequestDto request) {
        LogoutResponseDto response = authService.logout(request);
        return ResponseEntity.ok(response);
    }
}
