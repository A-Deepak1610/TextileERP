package com.textile.erp.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenResponseDto {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private long expiresIn;

    private String refreshToken;
}
