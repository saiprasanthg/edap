package com.edap.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String username;
    private Set<String> roles;
}
