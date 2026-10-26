package com.example.demo.repositories;

import com.example.demo.Enums.PropertyStatus;
import com.example.demo.entities.Property;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyRepository extends MongoRepository<Property, String> {

    // Recherches simples
    List<Property> findByOwnerId(String ownerId);
    List<Property> findByCity(String city);
    List<Property> findByCountry(String country);
    List<Property> findByStatus(PropertyStatus status);

    // Recherche combinée
    @Query("{ 'city': ?0, 'maxGuests': { $gte: ?1 } }")
    List<Property> findByCityAndMaxGuestsGreaterThanEqual(String city, Integer maxGuests);

    // Recherche par fourchette de prix
    @Query("{ 'pricePerNight': { $gte: ?0, $lte: ?1 } }")
    List<Property> findByPricePerNightBetween(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice);

    // Recherche avancée
    @Query("{ 'city': { $regex: ?0, $options: 'i' }, " +
            "'maxGuests': { $gte: ?1 }, " +
            "'pricePerNight': { $lte: ?2 }, " +
            "'status': 'AVAILABLE' }")
    List<Property> advancedSearch(String city, Integer guests, java.math.BigDecimal maxPrice);

    // Recherche par nombre de guests minimum
    List<Property> findByMaxGuestsGreaterThanEqual(Integer maxGuests);

    // Compter par propriétaire
    long countByOwnerId(String ownerId);

    // Compter par statut
    long countByStatus(PropertyStatus status);
}
