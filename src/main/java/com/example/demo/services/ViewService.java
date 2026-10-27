package com.example.demo.services;

import com.example.demo.dto.*;
import com.example.demo.entities.*;
import com.example.demo.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ViewService {
    private final UserRepository users;
    private final PropertyRepository properties;
    private final ReviewRepository reviews;
    private final AmenityRepository amenities;
    private String name(String id) { return users.findById(id).map(User::getUsername).orElse("Utilisateur"); }

    public PropertyView property(Property p) {
        var feedback = reviews.findByPropertyId(p.getId());
        var ids = p.getAmenityIds() == null ? List.<String>of() : p.getAmenityIds();
        return new PropertyView(p.getId(), p.getTitle(), p.getDescription(), p.getType(), p.getAddress(),
                p.getCity(), p.getCountry(), p.getLatitude(), p.getLongitude(), p.getPricePerNight(),
                p.getMaxGuests(), p.getBedrooms(), p.getStatus(), p.getOwnerId(), name(p.getOwnerId()), ids,
                amenities.findByIdIn(ids), p.getImageUrls() == null ? List.of() : p.getImageUrls(),
                feedback.stream().mapToInt(Review::getRating).average().orElse(0), feedback.size(), "MAD");
    }

    public BookingView booking(Booking b) {
        var p = properties.findById(b.getPropertyId()).orElse(null);
        return new BookingView(b.getId(), b.getPropertyId(), b.getUserId(), b.getStartDate(), b.getEndDate(),
                b.getNumberOfGuests(), b.getTotalPrice(), b.getStatus(), b.getExpiresAt(), b.getCreatedAt(),
                b.getGuestMessage(), b.getDecisionReason(), name(b.getUserId()), p == null ? "Logement" : p.getTitle(),
                p == null ? "" : p.getCity(), p == null || p.getImageUrls() == null || p.getImageUrls().isEmpty() ? null : p.getImageUrls().get(0),
                p == null ? null : p.getOwnerId(), p == null ? "Hôte" : name(p.getOwnerId()), "MAD");
    }
}
