package com.jiny.jwtauth.dto;

import lombok.Data;

@Data
public class TokenVO {
    private String accessToken;
    private String refreshToken;
}
