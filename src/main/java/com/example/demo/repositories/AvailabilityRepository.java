package com.example.demo.repositories;

import com.example.demo.entities.Availability;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvailabilityRepository extends MongoRepository<Availability, String> {

    // Trouver les disponibilités d'une propriété
    List<Availability> findByPropertyId(String propertyId);

    // Trouver la disponibilité pour une date spécifique
    Optional<Availability> findByPropertyIdAndDate(String propertyId, LocalDate date);

    // Trouver les disponibilités sur une période
    @Query("{ 'propertyId': ?0, 'date': { $gte: ?1, $lte: ?2 } }")
    List<Availability> findAvailabilitiesInRange(String propertyId, LocalDate startDate, LocalDate endDate);

    // Trouver les dates disponibles (isAvailable = true) sur une période
    @Query("{ 'propertyId': ?0, 'date': { $gte: ?1, $lte: ?2 }, 'isAvailable': true }")
    List<Availability> findAvailableDatesInRange(String propertyId, LocalDate startDate, LocalDate endDate);

    // Trouver les dates indisponibles
    @Query("{ 'propertyId': ?0, 'isAvailable': false }")
    List<Availability> findUnavailableDates(String propertyId);

    // Trouver les dates avec price override
    @Query("{ 'propertyId': ?0, 'priceOverride': { $ne: null } }")
    List<Availability> findDatesWithPriceOverride(String propertyId);

    // Trouver les prix spéciaux (priceOverride différent du prix de base)
    @Query("{ 'propertyId': ?0, 'date': { $gte: ?1, $lte: ?2 }, 'priceOverride': { $ne: null } }")
    List<Availability> findPriceOverridesInRange(String propertyId, LocalDate startDate, LocalDate endDate);

    // Compter les jours disponibles dans une période
    @Query(value = "{ 'propertyId': ?0, 'date': { $gte: ?1, $lte: ?2 }, 'isAvailable': true }",
            count = true)
    long countAvailableDaysInRange(String propertyId, LocalDate startDate, LocalDate endDate);

    // Supprimer toutes les disponibilités d'une propriété (utile pour réinitialiser)
    long deleteByPropertyId(String propertyId);

    // Trouver les disponibilités futures
    @Query("{ 'propertyId': ?0, 'date': { $gte: ?1 } }")
    List<Availability> findFutureAvailabilities(String propertyId, LocalDate currentDate);
}