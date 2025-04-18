package com.jiny.jwtauth.dto;

import lombok.Data;

@Data
public class CreateTokenDto {
    private String clientIp;
    private Long expired;
    private String payload;
}
