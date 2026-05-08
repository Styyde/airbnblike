package com.example.demo.repositories;
import com.example.demo.entities.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    // Recherche par email (pour l'authentification)
    Optional<User> findByEmail(String email);

    // Recherche par username
    Optional<User> findByUsername(String username);

    // Vérifier l'existence par email
    boolean existsByEmail(String email);

    // Vérifier l'existence par username
    boolean existsByUsername(String username);

    // Recherche par rôle (ex: trouver tous les admins)
    @Query("{ 'roles': ?0 }")
    List<User> findByRole(String role);

    // Recherche par email ou username (pour login avec les deux)
    @Query("{ $or: [ { 'email': ?0 }, { 'username': ?0 } ] }")
    Optional<User> findByEmailOrUsername(String login);

    // Compter les utilisateurs par rôle
    @Query(value = "{ 'roles': ?0 }", count = true)
    long countByRole(String role);

    // Recherche par début de nom (autocomplete)
    @Query("{ 'username': { $regex: ?0, $options: 'i' } }")
    List<User> searchByUsernameStartingWith(String prefix);
}
