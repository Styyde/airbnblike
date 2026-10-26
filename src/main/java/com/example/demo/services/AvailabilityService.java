package com.example.demo.services;

import com.example.demo.dto.AvailabilityRequestDTO;
import com.example.demo.entities.Availability;
import com.example.demo.repositories.AvailabilityRepository;
import com.example.demo.repositories.BookingRepository;
import com.example.demo.security.ResourceAccess;
import com.example.demo.exceptions.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {
    private final AvailabilityRepository availabilityRepository;
    private final BookingRepository bookingRepository;
    private final ResourceAccess access;
    private final PropertyCalendar calendar;

    @Transactional
    public Availability update(AvailabilityRequestDTO request) {
        access.propertyOwner(request.getPropertyId());
        calendar.lock(request.getPropertyId());
        if (Boolean.FALSE.equals(request.getIsAvailable()) && !bookingRepository.findConflictingBookings(
                request.getPropertyId(), request.getDate(), request.getDate().plusDays(1)).isEmpty()) {
            throw ApiException.conflict("Cette nuit est déjà réservée");
        }
        Availability day = availabilityRepository.findByPropertyIdAndDate(request.getPropertyId(), request.getDate())
                .orElse(new Availability(null, request.getPropertyId(), request.getDate(), true, null));
        if (request.getIsAvailable() != null) day.setIsAvailable(request.getIsAvailable());
        day.setPriceOverride(request.getPriceOverride());
        return availabilityRepository.save(day);
    }

    public List<Availability> getCalendar(String propertyId, LocalDate start, LocalDate end) {
        access.propertyOwner(propertyId);
        StayDates.validate(start, end);
        return availabilityRepository.findAvailabilitiesInRange(propertyId, start, end);
    }

    @Transactional
    public void blockDates(String propertyId, LocalDate start, LocalDate end) {
        StayDates.validate(start, end);
        access.propertyOwner(propertyId);
        calendar.lock(propertyId);
        if (!bookingRepository.findConflictingBookings(propertyId, start, end).isEmpty()) {
            throw ApiException.conflict("Des nuits sont déjà réservées");
        }
        for (LocalDate date = start; date.isBefore(end); date = date.plusDays(1)) {
            Availability day = availabilityRepository.findByPropertyIdAndDate(propertyId, date)
                    .orElse(new Availability(null, propertyId, date, true, null));
            day.setIsAvailable(false);
            availabilityRepository.save(day);
        }
    }

    @Transactional
    public void setPriceOverride(String propertyId, LocalDate date, BigDecimal price) {
        access.propertyOwner(propertyId);
        calendar.lock(propertyId);
        if (date == null || date.isBefore(LocalDate.now()) || price == null || price.signum() <= 0 || price.scale() > 2) {
            throw ApiException.badRequest("Date ou prix invalide");
        }
        Availability day = availabilityRepository.findByPropertyIdAndDate(propertyId, date)
                .orElse(new Availability(null, propertyId, date, true, null));
        day.setPriceOverride(price);
        availabilityRepository.save(day);
    }

    @Transactional
    public void generateAvailabilityCalendar(String propertyId, int months) {
        access.propertyOwner(propertyId);
        calendar.lock(propertyId);
        if (months < 1 || months > 12) throw ApiException.badRequest("Durée entre 1 et 12 mois requise");
        LocalDate end = LocalDate.now().plusMonths(months);
        for (LocalDate date = LocalDate.now(); date.isBefore(end); date = date.plusDays(1)) {
            if (availabilityRepository.findByPropertyIdAndDate(propertyId, date).isEmpty()) {
                availabilityRepository.save(new Availability(null, propertyId, date, true, null));
            }
        }
    }
}
