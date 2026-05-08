package com.example.demo.services;

import com.example.demo.Enums.Role;
import com.example.demo.entities.User;
import com.example.demo.dto.UserResponseDTO;
import com.example.demo.repositories.UserRepository;
import com.example.demo.repositories.PropertyRepository;  // ← Ajoute cet import
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PropertyRepository propertyRepository;  // ← AJOUTE CETTE LIGNE

    // Inscription
    public User registerUser(String username, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(Set.of(Role.ROLE_USER.toString()));

        return userRepository.save(user);
    }

    // Authentification
    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        return user;
    }

    // Ajouter un rôle
    public User addRole(String userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Set<String> roles = user.getRoles();
        roles.add(role.toString());
        user.setRoles(roles);

        return userRepository.save(user);
    }

    // Récupérer un utilisateur par son ID
    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Convertir en DTO
    public UserResponseDTO toDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRoles(user.getRoles());
        return dto;
    }

    // Récupérer tous les hôtes (propriétaires)
    public List<User> getAllHosts() {
        return userRepository.findAll().stream()
                .filter(user -> !propertyRepository.findByOwnerId(user.getId()).isEmpty())
                .collect(Collectors.toList());
    }
}