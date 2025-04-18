package com.jiny.jwtauth.dto;

import lombok.Data;

@Data
public class RefreshTokenDto {
    private String clientIp;
    private Long expired;
    private String accessToken;
    private String refreshToken;
}
