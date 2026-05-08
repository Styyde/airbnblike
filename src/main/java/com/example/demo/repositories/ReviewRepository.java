package com.example.demo.repositories;

import com.example.demo.entities.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {

    // Trouver les avis d'une propriété
    List<Review> findByPropertyId(String propertyId);

    // Trouver les avis d'un utilisateur
    List<Review> findByAuthorId(String authorId);

    // Vérifier si une réservation a déjà un avis
    boolean existsByBookingId(String bookingId);

    // Trouver l'avis d'une réservation spécifique
    Optional<Review> findByBookingId(String bookingId);

    // Trouver les meilleurs avis (rating >= 4)
    @Query("{ 'propertyId': ?0, 'rating': { $gte: 4 } }")
    List<Review> findTopReviewsByProperty(String propertyId);

    // Calculer la note moyenne d'une propriété
    @Query(value = "{ 'propertyId': ?0 }", fields = "{ 'rating': 1 }")
    List<Review> findRatingsByPropertyId(String propertyId);

    // Trouver les avis récents pour une propriété
    @Query(value = "{ 'propertyId': ?0 }", sort = "{ 'createdAt': -1 }")
    List<Review> findRecentReviewsByProperty(String propertyId);

    // Supprimer tous les avis d'une propriété (quand elle est supprimée)
    long deleteByPropertyId(String propertyId);

    // Compter les avis par note
    @Query(value = "{ 'propertyId': ?0, 'rating': ?1 }", count = true)
    long countByRating(String propertyId, Integer rating);

    // Supprimer tous les avis d'un utilisateur
    long deleteByAuthorId(String authorId);

    // Compter les avis d'une propriété
    long countByPropertyId(String propertyId);
    

    // Soft delete : trouver les avis non supprimés
    @Query("{ 'propertyId': ?0, 'isDeleted': false }")
    List<Review> findActiveReviewsByProperty(String propertyId);
}