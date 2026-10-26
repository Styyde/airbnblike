package com.example.demo.services;

import com.example.demo.Enums.Role;
import com.example.demo.entities.User;
import com.example.demo.dto.UserResponseDTO;
import com.example.demo.repositories.UserRepository;
import com.example.demo.repositories.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PropertyRepository propertyRepository;

    // Inscription
    public User registerUser(String username, String email, String password) {
        username = username.strip();
        email = email.strip().toLowerCase(java.util.Locale.ROOT);
        if (username.length() < 3) throw com.example.demo.exceptions.ApiException.badRequest("Nom d'utilisateur trop court");
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw com.example.demo.exceptions.ApiException.badRequest("Le mot de passe dépasse 72 octets");
        }
        if (userRepository.existsByEmail(email)) {
            throw com.example.demo.exceptions.ApiException.conflict("Email already exists");
        }
        if (userRepository.existsByUsername(username)) {
            throw com.example.demo.exceptions.ApiException.conflict("Username already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(new java.util.HashSet<>(Set.of(Role.ROLE_USER.toString())));

        return userRepository.save(user);
    }

    // Ajouter un rôle
    public User addRole(String userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> com.example.demo.exceptions.ApiException.notFound("User not found"));

        Set<String> roles = new java.util.HashSet<>(user.getRoles());
        roles.add(role.toString());
        user.setRoles(roles);

        return userRepository.save(user);
    }

    // Récupérer un utilisateur par son ID
    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> com.example.demo.exceptions.ApiException.notFound("User not found"));
    }

    // Convertir en DTO
    public UserResponseDTO toDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRoles(user.getRoles());
        dto.setActive(user.isActive());
        return dto;
    }

    // Récupérer tous les hôtes (propriétaires)
    public List<User> getAllHosts() {
        return userRepository.findByRole(Role.ROLE_OWNER.name());
    }

    /**
     * Récupérer tous les utilisateurs
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Assigner un rôle à un utilisateur
     */
    @Transactional
    public User assignRole(String userId, String role) {
        Role.valueOf(role);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> com.example.demo.exceptions.ApiException.notFound("User not found with id: " + userId));

        Set<String> roles = new java.util.HashSet<>(user.getRoles());
        roles.add(role);
        user.setRoles(roles);

        return userRepository.save(user);
    }

    /**
     * Désactiver un utilisateur en conservant l'historique métier.
     */
    @Transactional
    public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> com.example.demo.exceptions.ApiException.notFound("User not found with id: " + userId));

        user.setActive(false);
        userRepository.save(user);
        var owned = propertyRepository.findByOwnerId(userId);
        owned.forEach(property -> property.setStatus(com.example.demo.Enums.PropertyStatus.DELETED));
        propertyRepository.saveAll(owned);
    }

    /**
     * Compter le nombre total d'utilisateurs
     */
    public long countUsers() {
        return userRepository.count();
    }
}
