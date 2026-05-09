package com.example.demo.services;

import com.example.demo.Enums.BookingStatus;
import com.example.demo.Enums.PropertyStatus;
import com.example.demo.entities.Property;
import com.example.demo.entities.Availability;
import com.example.demo.dto.PropertyDTO;
import com.example.demo.repositories.PropertyRepository;
import com.example.demo.repositories.BookingRepository;
import com.example.demo.repositories.AvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final BookingRepository bookingRepository;
    private final AvailabilityRepository availabilityRepository;

    // ===== MÉTHODES CRUD DE BASE =====

    // Récupérer toutes les propriétés
    public List<Property> getAllProperties() {
        return propertyRepository.findAll();
    }

    // Récupérer toutes les propriétés disponibles
    public List<Property> getAllAvailableProperties() {
        return propertyRepository.findByStatus(PropertyStatus.AVAILABLE);
    }

    // Récupérer une propriété par son ID
    public Property getPropertyById(String id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Property not found with id: " + id));
    }

    // Créer une propriété
    @Transactional
    public Property createProperty(Property property) {
        property.setStatus(PropertyStatus.AVAILABLE);
        return propertyRepository.save(property);
    }

    // Mettre à jour une propriété
    @Transactional
    public Property updateProperty(String id, Property updatedProperty) {
        Property existingProperty = getPropertyById(id);

        existingProperty.setTitle(updatedProperty.getTitle());
        existingProperty.setDescription(updatedProperty.getDescription());
        existingProperty.setType(updatedProperty.getType());
        existingProperty.setAddress(updatedProperty.getAddress());
        existingProperty.setCity(updatedProperty.getCity());
        existingProperty.setCountry(updatedProperty.getCountry());
        existingProperty.setLatitude(updatedProperty.getLatitude());
        existingProperty.setLongitude(updatedProperty.getLongitude());
        existingProperty.setPricePerNight(updatedProperty.getPricePerNight());
        existingProperty.setMaxGuests(updatedProperty.getMaxGuests());
        existingProperty.setBedrooms(updatedProperty.getBedrooms());
        existingProperty.setAmenityIds(updatedProperty.getAmenityIds());

        return propertyRepository.save(existingProperty);
    }

    // Supprimer une propriété (soft delete)
    @Transactional
    public void deleteProperty(String id) {
        Property property = getPropertyById(id);
        property.setStatus(PropertyStatus.DELETED);
        propertyRepository.save(property);
    }

    // Supprimer définitivement une propriété (admin seulement)
    @Transactional
    public void hardDeleteProperty(String id) {
        Property property = getPropertyById(id);
        // Supprimer aussi les disponibilités associées
        availabilityRepository.deleteByPropertyId(id);
        propertyRepository.delete(property);
    }

    // ===== MÉTHODES EXISTANTES =====

    // Vérifier la disponibilité d'un logement sur une période
    public boolean isAvailable(String propertyId, LocalDate startDate, LocalDate endDate) {
        Property property = getPropertyById(propertyId);

        if (property.getStatus() != PropertyStatus.AVAILABLE) {
            return false;
        }

        // Vérifier les réservations existantes
        boolean hasConflictingBooking = bookingRepository.findConflictingBookings(
                        propertyId, startDate, endDate).stream()
                .anyMatch(booking -> booking.getStatus() == BookingStatus.CONFIRMED ||
                        booking.getStatus() == BookingStatus.PENDING);

        if (hasConflictingBooking) {
            return false;
        }

        // Vérifier les indisponibilités dans Availability
        List<Availability> availabilities = availabilityRepository.findAvailabilitiesInRange(
                propertyId, startDate, endDate);

        for (Availability availability : availabilities) {
            if (!availability.getIsAvailable()) {
                return false;
            }
        }

        return true;
    }

    // Calculer le prix total avec les price overrides
    public Double calculateTotalPrice(String propertyId, LocalDate startDate, LocalDate endDate) {
        Property property = getPropertyById(propertyId);

        long nights = ChronoUnit.DAYS.between(startDate, endDate);
        double basePrice = nights * property.getPricePerNight();

        // Appliquer les price overrides
        List<Availability> availabilities = availabilityRepository.findAvailabilitiesInRange(
                propertyId, startDate, endDate);

        double totalPrice = basePrice;
        for (Availability availability : availabilities) {
            if (availability.getPriceOverride() != null) {
                totalPrice = totalPrice - property.getPricePerNight() + availability.getPriceOverride();
            }
        }

        return totalPrice;
    }

    // Récupérer tous les logements d'un propriétaire
    public List<Property> getPropertiesByOwner(String ownerId) {
        return propertyRepository.findByOwnerId(ownerId);
    }

    // Recherche simple par ville
    public List<Property> searchByCity(String city) {
        return propertyRepository.findByCity(city);
    }

    // Recherche par ville et nombre de guests
    public List<Property> searchByCityAndGuests(String city, Integer guests) {
        return propertyRepository.findByCityAndMaxGuestsGreaterThanEqual(city, guests);
    }

    // Mettre à jour le statut
    @Transactional
    public Property updateStatus(String propertyId, PropertyStatus status) {
        Property property = getPropertyById(propertyId);
        property.setStatus(status);
        return propertyRepository.save(property);
    }

    // ===== MÉTHODES DE RECHERCHE AVANCÉE =====

    // Recherche par fourchette de prix
    public List<Property> searchByPriceRange(Double minPrice, Double maxPrice) {
        return propertyRepository.findByPricePerNightBetween(minPrice, maxPrice);
    }

    // Recherche multicritères
    public List<Property> advancedSearch(String city, Integer guests, Double maxPrice) {
        if (city != null && guests != null && maxPrice != null) {
            return propertyRepository.advancedSearch(city, guests, maxPrice);
        } else if (city != null && guests != null) {
            return propertyRepository.findByCityAndMaxGuestsGreaterThanEqual(city, guests);
        } else if (city != null) {
            return propertyRepository.findByCity(city);
        } else if (guests != null) {
            return propertyRepository.findByMaxGuestsGreaterThanEqual(guests);
        }
        return propertyRepository.findAll();
    }

    // ===== MÉTHODES STATISTIQUES =====

    // Compter le nombre de propriétés d'un propriétaire
    public long countPropertiesByOwner(String ownerId) {
        return propertyRepository.countByOwnerId(ownerId);
    }

    // Compter le nombre total de propriétés disponibles
    public long countAvailableProperties() {
        return propertyRepository.countByStatus(PropertyStatus.AVAILABLE);
    }

    // Convertir Property en PropertyDTO
    public PropertyDTO toDTO(Property property, List<String> imageUrls, List<String> amenityNames) {
        PropertyDTO dto = new PropertyDTO();
        dto.setId(property.getId());
        dto.setTitle(property.getTitle());
        dto.setDescription(property.getDescription());
        dto.setType(property.getType());
        dto.setCity(property.getCity());
        dto.setCountry(property.getCountry());
        dto.setPricePerNight(property.getPricePerNight());
        dto.setMaxGuests(property.getMaxGuests());
        dto.setImageUrls(imageUrls);
        dto.setAmenities(amenityNames);
        return dto;
    }
    // Ajoutez ces méthodes dans PropertyService.java

    /**
     * Compter le nombre total de propriétés (tous statuts confondus)
     */
    public long countAllProperties() {
        return propertyRepository.count();
    }

    /**
     * Compter le nombre de propriétés par statut
     */
    public long countPropertiesByStatus(PropertyStatus status) {
        return propertyRepository.countByStatus(status);
    }
}