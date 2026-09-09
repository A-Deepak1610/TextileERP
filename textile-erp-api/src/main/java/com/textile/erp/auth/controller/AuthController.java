package com.textile.erp.auth.controller;

import com.textile.erp.auth.dto.LoginRequestDto;
import com.textile.erp.auth.dto.LoginResponseDto;
import com.textile.erp.auth.dto.LogoutRequestDto;
import com.textile.erp.auth.dto.LogoutResponseDto;
import com.textile.erp.auth.dto.RefreshTokenRequestDto;
import com.textile.erp.auth.dto.RefreshTokenResponseDto;
import com.textile.erp.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponseDto> refresh(@RequestBody RefreshTokenRequestDto request) {
        RefreshTokenResponseDto response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponseDto> logout(@RequestBody(required = false) LogoutRequestDto request) {
        LogoutResponseDto response = authService.logout(request);
        return ResponseEntity.ok(response);
    }
}
