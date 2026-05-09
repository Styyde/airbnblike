package com.example.demo.repositories;

import java.time.LocalDateTime;
import java.util.List;
import com.example.demo.Enums.BookingStatus;
import com.example.demo.entities.Booking;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {

    // Trouver les réservations d'un utilisateur
    List<Booking> findByUserId(String userId);

    // Trouver les réservations d'une propriété
    List<Booking> findByPropertyId(String propertyId);

    // Trouver par statut
    List<Booking> findByStatus(BookingStatus status);

    // Trouver les réservations par statut et les compter
    long countByStatus(BookingStatus status);

    // Trouver les réservations actives (confirmées et non terminées)
    @Query("{ 'propertyId': ?0, 'status': 'CONFIRMED', 'endDate': { $gte: ?1 } }")
    List<Booking> findActiveBookings(String propertyId, LocalDate currentDate);

    // Trouver les réservations qui chevauchent une période (pour vérifier disponibilité)
    @Query("{ 'propertyId': ?0, 'status': { $in: ['CONFIRMED', 'PENDING'] }, " +
            "'startDate': { $lt: ?2 }, 'endDate': { $gt: ?1 } }")
    List<Booking> findConflictingBookings(String propertyId, LocalDate startDate, LocalDate endDate);

    // Trouver les réservations futures d'un utilisateur
    @Query("{ 'userId': ?0, 'status': 'CONFIRMED', 'startDate': { $gte: ?1 } }")
    List<Booking> findFutureBookings(String userId, LocalDate currentDate);

    // Trouver les réservations passées d'un utilisateur
    @Query("{ 'userId': ?0, 'status': 'COMPLETED', 'endDate': { $lt: ?1 } }")
    List<Booking> findPastBookings(String userId, LocalDate currentDate);

    // Trouver les réservations à confirmer (PENDING)
    @Query("{ 'status': 'PENDING', 'createdAt': { $lte: ?0 } }")
    List<Booking> findPendingBookingsOlderThan(LocalDate date);

    // Compter les réservations par propriété
    @Query(value = "{ 'propertyId': ?0, 'status': 'CONFIRMED' }", count = true)
    long countConfirmedBookingsByProperty(String propertyId);

    // Trouver les réservations par période (pour analytics)
    @Query("{ 'startDate': { $gte: ?0, $lte: ?1 }, 'status': 'CONFIRMED' }")
    List<Booking> findBookingsInPeriod(LocalDate startDate, LocalDate endDate);

    // Calculer le chiffre d'affaires total pour un propriétaire
    @Query(value = "{ 'ownerId': ?0, 'status': 'COMPLETED' }",
            fields = "{ 'totalPrice': 1 }")
    List<Booking> findCompletedBookingsByOwner(String ownerId);

    // ===== NOUVELLES MÉTHODES POUR L'ADMIN =====

    /**
     * Trouver les réservations par statuts et période de création
     */
    @Query("{ 'status': { $in: ?0 }, 'createdAt': { $gte: ?1, $lte: ?2 } }")
    List<Booking> findByStatusInAndCreatedAtBetween(List<BookingStatus> statuses,
                                                    LocalDateTime startDate,
                                                    LocalDateTime endDate);

    /**
     * Calculer la somme totale des prix pour des statuts spécifiques (pour le revenu)
     */
    @Aggregation(pipeline = {
            "{ $match: { 'status': { $in: ?0 } } }",
            "{ $group: { _id: null, total: { $sum: '$totalPrice' } } }"
    })
    Double sumTotalPriceByStatuses(List<BookingStatus> statuses);

    /**
     * Trouver les réservations par année (alternative sans $expr)
     */
    @Query("{ 'createdAt': { $gte: ?0, $lte: ?1 } }")
    List<Booking> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
}