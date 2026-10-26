package com.example.demo.controllers;

import com.example.demo.annotations.CurrentUser;
import com.example.demo.dto.ReviewRequestDTO;
import com.example.demo.entities.Review;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.services.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Review> addReview(
            @Valid @RequestBody ReviewRequestDTO request,
            @CurrentUser CustomUserDetails currentUser) {

        Review review = reviewService.addReview(
                request.getBookingId(),
                request.getRating(),
                request.getComment(),
                currentUser.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReview(
            @PathVariable String reviewId,
            @CurrentUser CustomUserDetails currentUser) {

        reviewService.deleteReviewByAuthor(reviewId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/can-review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> canUserReview(
            @RequestParam String bookingId,
            @CurrentUser CustomUserDetails currentUser) {

        boolean canReview = reviewService.canUserReview(bookingId, currentUser.getId());
        return ResponseEntity.ok(canReview);
    }

    @GetMapping("/property/{propertyId}")
    public java.util.List<com.example.demo.dto.ReviewDTO> byProperty(@PathVariable String propertyId) {
        return reviewService.getReviewsByProperty(propertyId);
    }
}
