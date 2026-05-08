package com.example.demo.dto;

import lombok.Data;
import java.util.Set;

@Data
public class UserResponseDTO {
    private String id;
    private String username;
    private String email;
    private Set<String> roles;
}