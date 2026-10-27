package com.example.demo.services;

import com.example.demo.Enums.*;
import com.example.demo.entities.*;
import com.example.demo.repositories.*;
import com.example.demo.exceptions.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CatalogService {
    private final PropertyRepository properties;
    private final PropertyService service;
    private final AvailabilityRepository availability;
    private final BookingRepository bookings;

    public List<Property> search(String city, LocalDate start, LocalDate end, Integer guests, PropertyType type, BigDecimal min, BigDecimal max) {
        if ((start == null) != (end == null)) throw ApiException.badRequest("Choisissez une date d’arrivée et de départ.");
        if (start != null) StayDates.validate(start, end);
        if (guests != null && guests < 1 || min != null && min.signum() < 0 || max != null && max.signum() < 0
                || min != null && max != null && min.compareTo(max) > 0) throw ApiException.badRequest("Filtres de recherche invalides.");
        return properties.findByStatus(PropertyStatus.AVAILABLE).stream()
                .filter(p -> city == null || city.isBlank() || p.getCity().toLowerCase(Locale.ROOT).contains(city.strip().toLowerCase(Locale.ROOT)))
                .filter(p -> guests == null || p.getMaxGuests() >= guests)
                .filter(p -> type == null || p.getType() == type)
                .filter(p -> min == null || p.getPricePerNight().compareTo(min) >= 0)
                .filter(p -> max == null || p.getPricePerNight().compareTo(max) <= 0)
                .filter(p -> start == null || service.isAvailable(p.getId(), start, end)).toList();
    }
    public Property published(String id) {
        Property p = service.getPropertyById(id);
        if (p.getStatus() != PropertyStatus.AVAILABLE) throw ApiException.notFound("Logement introuvable.");
        return p;
    }
    public record Day(LocalDate date, boolean available, BigDecimal price) {}
    public List<Day> calendar(String id, LocalDate start, LocalDate end) {
        var property = published(id);
        StayDates.validate(start, end);
        Map<LocalDate,Availability> exceptions = new HashMap<>();
        availability.findAvailabilitiesInRange(id, start, end).forEach(a -> exceptions.put(a.getDate(), a));
        var occupied = bookings.findConflictingBookings(id, start, end).stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED
                || b.getExpiresAt() == null || b.getExpiresAt().isAfter(Instant.now())).toList();
        List<Day> days = new ArrayList<>();
        for (LocalDate date = start; date.isBefore(end); date = date.plusDays(1)) {
            final LocalDate day = date;
            var exception = exceptions.get(day);
            boolean free = (exception == null || !Boolean.FALSE.equals(exception.getIsAvailable()))
                    && occupied.stream().noneMatch(b -> !day.isBefore(b.getStartDate()) && day.isBefore(b.getEndDate()));
            days.add(new Day(day, free, exception != null && exception.getPriceOverride() != null ? exception.getPriceOverride() : property.getPricePerNight()));
        }
        return days;
    }
    public record Quote(long nights, BigDecimal totalPrice, String currency, boolean available, List<Day> days) {}
    public Quote quote(String id, LocalDate start, LocalDate end, int guests) {
        var p = published(id);
        if (guests < 1 || guests > p.getMaxGuests()) throw ApiException.badRequest("Nombre de voyageurs incompatible avec ce logement.");
        var days = calendar(id, start, end);
        return new Quote(ChronoUnit.DAYS.between(start,end), days.stream().map(Day::price).reduce(BigDecimal.ZERO,BigDecimal::add),
                "MAD", days.stream().allMatch(Day::available), days);
    }
}
