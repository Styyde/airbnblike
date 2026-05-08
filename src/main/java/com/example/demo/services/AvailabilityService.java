package com.example.demo.services;

import com.example.demo.entities.Availability;
import com.example.demo.repositories.AvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;

    // Bloquer des dates (maintenance, indisponibilité)
    @Transactional
    public void blockDates(String propertyId, LocalDate startDate, LocalDate endDate) {
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            Availability availability = availabilityRepository.findByPropertyIdAndDate(propertyId, currentDate)
                    .orElse(new Availability(null, propertyId, currentDate, true, null));

            availability.setIsAvailable(false);
            availabilityRepository.save(availability);

            currentDate = currentDate.plusDays(1);
        }
    }

    // Définir un prix spécial pour une date
    @Transactional
    public void setPriceOverride(String propertyId, LocalDate date, Double price) {
        Availability availability = availabilityRepository.findByPropertyIdAndDate(propertyId, date)
                .orElse(new Availability(null, propertyId, date, true, null));

        availability.setPriceOverride(price);
        availabilityRepository.save(availability);
    }

    // Générer le calendrier des disponibilités pour les 3 prochains mois
    @Transactional
    public void generateAvailabilityCalendar(String propertyId, int months) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(months);

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            if (availabilityRepository.findByPropertyIdAndDate(propertyId, currentDate).isEmpty()) {
                Availability availability = new Availability(null, propertyId, currentDate, true, null);
                availabilityRepository.save(availability);
            }
            currentDate = currentDate.plusDays(1);
        }
    }
}