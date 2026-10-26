package com.example.demo.controllers;

import com.example.demo.Enums.Role;
import com.example.demo.dto.UserResponseDTO;
import com.example.demo.security.ResourceAccess;
import com.example.demo.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {
    private final ResourceAccess access;
    private final UserService service;

    @GetMapping
    public UserResponseDTO me() { return service.toDTO(service.getUserById(access.current().getId())); }

    @PostMapping("/host")
    public UserResponseDTO becomeHost() {
        return service.toDTO(service.addRole(access.current().getId(), Role.ROLE_OWNER));
    }
}
