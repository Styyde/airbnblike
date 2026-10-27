package com.example.demo.controllers;

import com.example.demo.dto.*;
import com.example.demo.security.ResourceAccess;
import com.example.demo.services.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService service;
    private final ResourceAccess access;
    private final ViewService views;

    @PostMapping
    public ResponseEntity<BookingView> create(@Valid @RequestBody BookingRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(views.booking(service.createBooking(request)));
    }
    @GetMapping("/user/me")
    public List<BookingView> mine() { return service.getBookingsByUser(access.current().getId()).stream().map(views::booking).toList(); }
    @GetMapping("/host/me")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    public List<BookingView> host() { return service.getHostBookings().stream().map(views::booking).toList(); }
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.id")
    public List<BookingView> byUser(@PathVariable String userId) { return service.getBookingsByUser(userId).stream().map(views::booking).toList(); }
    @GetMapping("/property/{propertyId}")
    public List<BookingView> byProperty(@PathVariable String propertyId) {
        access.propertyOwner(propertyId); return service.getBookingsByProperty(propertyId).stream().map(views::booking).toList();
    }
    @GetMapping("/{id}")
    public BookingView one(@PathVariable String id) { return views.booking(access.bookingParticipant(id)); }
    @PutMapping("/{id}/accept")
    public BookingView accept(@PathVariable String id) { return views.booking(service.accept(id)); }
    public record Decision(@Size(max=500) String reason) {}
    @PutMapping("/{id}/reject")
    public BookingView reject(@PathVariable String id, @Valid @RequestBody(required=false) Decision decision) {
        return views.booking(service.reject(id, decision == null ? null : decision.reason()));
    }
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable String id) { service.cancelBooking(id); return ResponseEntity.noContent().build(); }
    @PutMapping("/{id}/complete")
    public BookingView complete(@PathVariable String id) { return views.booking(service.completeBooking(id)); }
}
