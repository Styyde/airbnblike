package com.example.demo.security;

import com.example.demo.entities.Booking;
import com.example.demo.repositories.BookingRepository;
import com.example.demo.repositories.PropertyRepository;
import com.example.demo.exceptions.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceAccess {
    private final BookingRepository bookings;
    private final PropertyRepository properties;

    public CustomUserDetails current() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            throw new AccessDeniedException("Authentification requise");
        }
        return user;
    }

    public boolean isAdmin() { return current().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")); }
    public void self(String userId) {
        if (!current().getId().equals(userId)) throw new AccessDeniedException("Utilisateur non autorisé");
    }
    public void propertyOwner(String propertyId) {
        var property = properties.findById(propertyId).orElseThrow(() -> ApiException.notFound("Logement introuvable"));
        if (!property.getOwnerId().equals(current().getId()) && !isAdmin()) throw new AccessDeniedException("Propriétaire requis");
    }
    public Booking bookingGuest(String bookingId) {
        var booking = bookings.findById(bookingId).orElseThrow(() -> ApiException.notFound("Réservation introuvable"));
        self(booking.getUserId());
        return booking;
    }
    public Booking bookingParticipant(String bookingId) {
        var booking = bookings.findById(bookingId).orElseThrow(() -> ApiException.notFound("Réservation introuvable"));
        if (!booking.getUserId().equals(current().getId())) propertyOwner(booking.getPropertyId());
        return booking;
    }
}
