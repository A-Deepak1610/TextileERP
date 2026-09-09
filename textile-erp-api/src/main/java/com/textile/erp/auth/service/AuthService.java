package com.textile.erp.auth.service;

import com.textile.erp.auth.dto.LoginRequestDto;
import com.textile.erp.auth.dto.LoginResponseDto;
import com.textile.erp.auth.dto.LogoutRequestDto;
import com.textile.erp.auth.dto.LogoutResponseDto;
import com.textile.erp.auth.dto.RefreshTokenRequestDto;
import com.textile.erp.auth.dto.RefreshTokenResponseDto;

public interface AuthService {

    LoginResponseDto login(LoginRequestDto request);

    RefreshTokenResponseDto refreshToken(RefreshTokenRequestDto request);

    LogoutResponseDto logout(LogoutRequestDto request);
}
