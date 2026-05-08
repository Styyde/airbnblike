package com.example.demo.controllers;

import com.example.demo.dto.ReviewDTO;
import com.example.demo.dto.ReviewRequestDTO;
import com.example.demo.entities.Review;
import com.example.demo.services.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Ajouter un avis
     */
    @PostMapping
    public ResponseEntity<Review> addReview(@RequestBody ReviewRequestDTO request) {
        Review review = reviewService.addReview(
                request.getBookingId(),
                request.getRating(),
                request.getComment(),
                request.getAuthorId()
        );
        return ResponseEntity.ok(review);
    }

    /**
     * Récupérer tous les avis d'une propriété
     */
    @GetMapping("/property/{propertyId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByProperty(@PathVariable String propertyId) {
        return ResponseEntity.ok(reviewService.getReviewsByProperty(propertyId));
    }

    /**
     * Récupérer la note moyenne d'une propriété
     */
    @GetMapping("/property/{propertyId}/average-rating")
    public ResponseEntity<Double> getAverageRating(@PathVariable String propertyId) {
        double averageRating = reviewService.getAverageRating(propertyId);
        return ResponseEntity.ok(averageRating);
    }

    /**
     * Récupérer tous les avis d'un utilisateur
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(reviewService.getReviewsByAuthor(userId));
    }

    /**
     * Récupérer un avis par son ID
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewDTO> getReviewById(@PathVariable String reviewId) {
        Review review = reviewService.getReviewById(reviewId);
        ReviewDTO dto = reviewService.toDTO(review);
        return ResponseEntity.ok(dto);
    }

    /**
     * Supprimer un avis (par l'auteur)
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable String reviewId,
            @RequestParam String userId) {
        reviewService.deleteReviewByAuthor(reviewId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Vérifier si un utilisateur peut laisser un avis
     */
    @GetMapping("/can-review")
    public ResponseEntity<Boolean> canUserReview(
            @RequestParam String bookingId,
            @RequestParam String userId) {
        boolean canReview = reviewService.canUserReview(bookingId, userId);
        return ResponseEntity.ok(canReview);
    }
}