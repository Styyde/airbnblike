package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class LoginResponseDTO {
    private String id;
    private String username;
    private String email;
    private Set<String> roles;
    private String token;
    private String tokenType;
    private Long expiresIn;
}