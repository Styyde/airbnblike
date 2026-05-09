package com.example.demo.controllers;

import com.example.demo.annotations.CurrentUser;
import com.example.demo.dto.BookingRequestDTO;
import com.example.demo.entities.Booking;
import com.example.demo.security.CustomUserDetails;
import com.example.demo.services.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Booking> createBooking(
            @Valid @RequestBody BookingRequestDTO request,
            @CurrentUser CustomUserDetails currentUser) {

        // Vérifier que l'utilisateur crée sa propre réservation
        if (!request.getUserId().equals(currentUser.getId())) {
            throw new RuntimeException("Cannot create booking for another user");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request));
    }

    @GetMapping("/user/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Booking>> getMyBookings(@CurrentUser CustomUserDetails currentUser) {
        return ResponseEntity.ok(bookingService.getBookingsByUser(currentUser.getId()));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<List<Booking>> getBookingsByUser(
            @PathVariable String userId,
            @CurrentUser CustomUserDetails currentUser) {
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId));
    }

    @GetMapping("/property/{propertyId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Booking>> getBookingsByProperty(@PathVariable String propertyId) {
        return ResponseEntity.ok(bookingService.getBookingsByProperty(propertyId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Booking> getBookingById(@PathVariable String id, @CurrentUser CustomUserDetails currentUser) {
        Booking booking = bookingService.getBookingById(id);

        // Vérifier que l'utilisateur a le droit de voir cette réservation
        if (!booking.getUserId().equals(currentUser.getId()) && !currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new RuntimeException("Access denied to this booking");
        }

        return ResponseEntity.ok(booking);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelBooking(@PathVariable String id, @CurrentUser CustomUserDetails currentUser) {
        Booking booking = bookingService.getBookingById(id);

        if (!booking.getUserId().equals(currentUser.getId())) {
            throw new RuntimeException("Only the booking owner can cancel this booking");
        }

        bookingService.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Booking> confirmBooking(@PathVariable String id) {
        return ResponseEntity.ok(bookingService.confirmBooking(id));
    }
}