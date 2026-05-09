package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AuthResponseDTO {
    private boolean success;
    private String message;
    private LocalDateTime timestamp;
    private LoginResponseDTO data;
}