package com.example.demo.services;

import com.example.demo.entities.Property;
import com.example.demo.repositories.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final PropertyRepository propertyRepository;
    private final PropertyService propertyService;

    // Recherche multicritères
    public List<Property> searchProperties(String city, LocalDate startDate, LocalDate endDate,
                                           Integer guests, java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice) {
        StayDates.validate(startDate, endDate);
        if (city == null || city.isBlank() || guests == null || guests < 1
                || (minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)
                || (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)) {
            throw com.example.demo.exceptions.ApiException.badRequest("Critères de recherche invalides");
        }
        List<Property> properties = propertyRepository.findByCity(city);

        return properties.stream()
                .filter(property -> property.getMaxGuests() >= guests)
                .filter(property -> propertyService.isAvailable(property.getId(), startDate, endDate))
                .filter(property -> minPrice == null || property.getPricePerNight().compareTo(minPrice) >= 0)
                .filter(property -> maxPrice == null || property.getPricePerNight().compareTo(maxPrice) <= 0)
                .collect(Collectors.toList());
    }
}
