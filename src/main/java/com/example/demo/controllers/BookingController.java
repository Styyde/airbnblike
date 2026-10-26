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
public class BookingController {

    private final BookingService bookingService;
    private final com.example.demo.security.ResourceAccess access;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Booking> createBooking(
            @Valid @RequestBody BookingRequestDTO request,
            @CurrentUser CustomUserDetails currentUser) {

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
        access.propertyOwner(propertyId);
        return ResponseEntity.ok(bookingService.getBookingsByProperty(propertyId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Booking> getBookingById(@PathVariable String id, @CurrentUser CustomUserDetails currentUser) {
        Booking booking = access.bookingParticipant(id);

        return ResponseEntity.ok(booking);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelBooking(@PathVariable String id, @CurrentUser CustomUserDetails currentUser) {
        bookingService.cancelBooking(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Booking> completeBooking(@PathVariable String id) {
        return ResponseEntity.ok(bookingService.completeBooking(id));
    }
}
