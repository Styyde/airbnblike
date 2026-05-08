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
                                           Integer guests, Double minPrice, Double maxPrice) {
        List<Property> properties = propertyRepository.findByCity(city);

        return properties.stream()
                .filter(property -> property.getMaxGuests() >= guests)
                .filter(property -> propertyService.isAvailable(property.getId(), startDate, endDate))
                .filter(property -> minPrice == null || property.getPricePerNight() >= minPrice)
                .filter(property -> maxPrice == null || property.getPricePerNight() <= maxPrice)
                .collect(Collectors.toList());
    }
}