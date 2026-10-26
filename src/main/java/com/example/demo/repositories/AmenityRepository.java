package com.example.demo.repositories;

import com.example.demo.entities.Amenity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmenityRepository extends MongoRepository<Amenity, String> {

    // Trouver par nom (unique)
    Optional<Amenity> findByName(String name);

    // Trouver par catégorie
    List<Amenity> findByCategory(String category);

    // Recherche par nom (insensible à la casse)
    @Query("{ 'name': { $regex: ?0, $options: 'i' } }")
    List<Amenity> searchByName(String keyword);

    // Trouver par liste d'IDs
    List<Amenity> findByIdIn(List<String> ids);

    // Compter par catégorie
    @Query(value = "{ 'category': ?0 }", count = true)
    long countByCategory(String category);

    // Trouver les équipements essentiels (catégorie "Essentials")
    List<Amenity> findByCategoryOrderByName(String category);
}
