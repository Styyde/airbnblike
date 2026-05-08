package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data  // ← Cette annotation génère tous les getters/setters
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

    private String id;
    private String username;
    private String email;
    private Set<String> roles;
    private String token;
    private String tokenType;
}