package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;

@Document(collection = "users") // Nom de la collection dans MongoDB
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String id;

    @NotBlank
    @Indexed(unique = true) // Empêche les doublons de noms d'utilisateur
    private String username;

    @NotBlank
    @Email
    @Indexed(unique = true)
    private String email;

    @NotBlank
    @com.fasterxml.jackson.annotation.JsonIgnore
    @lombok.ToString.Exclude
    private String password;

    private boolean active = true;
    @org.springframework.data.annotation.Version
    private Long version;

    private Set<String> roles; // Exemple: ROLE_USER, ROLE_ADMIN
}
