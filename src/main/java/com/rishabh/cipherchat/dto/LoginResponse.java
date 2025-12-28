package com.rishabh.cipherchat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String refreshToken;
}
