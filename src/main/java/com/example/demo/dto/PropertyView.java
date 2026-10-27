package com.example.demo.dto;
import com.example.demo.Enums.*;
import java.math.BigDecimal;
import java.util.List;
import com.example.demo.entities.Amenity;

public record PropertyView(String id, String title, String description, PropertyType type, String address,
        String city, String country, Double latitude, Double longitude, BigDecimal pricePerNight,
        Integer maxGuests, Integer bedrooms, PropertyStatus status, String ownerId, String ownerName,
        List<String> amenityIds, List<Amenity> amenities, List<String> imageUrls,
        Double averageRating, long reviewCount, String currency) {}
