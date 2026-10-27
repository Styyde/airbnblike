package com.example.demo.services;

import com.example.demo.entities.Review;
import com.example.demo.entities.Booking;
import com.example.demo.Enums.BookingStatus;
import com.example.demo.dto.ReviewDTO;
import com.example.demo.repositories.ReviewRepository;
import com.example.demo.repositories.BookingRepository;
import com.example.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    // Ajouter un avis (seulement après un séjour complété)
    public Review addReview(String bookingId, Integer rating, String comment, String authorId) {
        if (rating == null || rating < 1 || rating > 5 || comment == null || comment.isBlank()) {
            throw com.example.demo.exceptions.ApiException.badRequest("Avis invalide");
        }
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> com.example.demo.exceptions.ApiException.notFound("Booking not found"));

        if (booking.getStatus() != BookingStatus.COMPLETED || booking.getEndDate().isAfter(java.time.LocalDate.now())) {
            throw com.example.demo.exceptions.ApiException.conflict("Can only review completed stays");
        }

        if (!booking.getUserId().equals(authorId)) {
            throw new org.springframework.security.access.AccessDeniedException("Only the guest can leave a review");
        }

        if (reviewRepository.existsByBookingId(bookingId)) {
            throw com.example.demo.exceptions.ApiException.conflict("Review already exists for this booking");
        }

        Review review = Review.builder()
                .bookingId(bookingId)
                .propertyId(booking.getPropertyId())
                .authorId(authorId)
                .rating(rating)
                .comment(comment)
                .createdAt(LocalDateTime.now())
                .build();

        return reviewRepository.save(review);
    }

    // Récupérer tous les avis d'une propriété
    public List<ReviewDTO> getReviewsByProperty(String propertyId) {
        return reviewRepository.findByPropertyId(propertyId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Récupérer tous les avis d'un utilisateur
    public List<ReviewDTO> getReviewsByAuthor(String authorId) {
        return reviewRepository.findByAuthorId(authorId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Récupérer un avis par son ID
    public Review getReviewById(String reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> com.example.demo.exceptions.ApiException.notFound("Review not found with id: " + reviewId));
    }

    // Calculer la note moyenne d'une propriété
    public Double getAverageRating(String propertyId) {
        List<Review> reviews = reviewRepository.findByPropertyId(propertyId);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }

    // Supprimer un avis (par l'auteur)
    public void deleteReviewByAuthor(String reviewId, String userId) {
        Review review = getReviewById(reviewId);

        if (!review.getAuthorId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only delete your own reviews");
        }

        reviewRepository.delete(review);
    }

    // Vérifier si un utilisateur peut laisser un avis
    public boolean canUserReview(String bookingId, String userId) {
        return bookingRepository.findById(bookingId)
                .map(booking -> booking.getStatus() == BookingStatus.COMPLETED
                        && !booking.getEndDate().isAfter(java.time.LocalDate.now())
                        && booking.getUserId().equals(userId)
                        && !reviewRepository.existsByBookingId(bookingId))
                .orElse(false);
    }

    // Convertir en DTO (public)
    public ReviewDTO toDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setAuthorId(review.getAuthorId());
        userRepository.findById(review.getAuthorId()).ifPresent(user ->
                dto.setAuthorName(user.getUsername()));
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}
