package com.example.demo.services;

import com.example.demo.Enums.BookingStatus;
import com.example.demo.entities.Booking;
import com.example.demo.dto.BookingRequestDTO;
import com.example.demo.repositories.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PropertyService propertyService; // Pour vérifier la disponibilité

    public Booking createBooking(BookingRequestDTO dto) {
        // 1. Vérifier disponibilité (Cas d'utilisation "Vérifier disponibilité")
        if (!propertyService.isAvailable(dto.getPropertyId(), dto.getStartDate(), dto.getEndDate())) {
            throw new RuntimeException("Logement non disponible pour ces dates");
        }

        // 2. Calculer le prix total (Logique métier)
        Double totalPrice = propertyService.calculateTotalPrice(dto.getPropertyId(), dto.getStartDate(), dto.getEndDate());

        // 3. Utilisation du Builder pour créer la réservation
        Booking booking = Booking.builder()
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .numberOfGuests(dto.getNumberOfGuests())
                .propertyId(dto.getPropertyId())
                .userId(dto.getUserId())
                .totalPrice(totalPrice)
                .status(BookingStatus.PENDING) // Statut initial avant paiement
                .createdAt(LocalDateTime.now())
                .build();

        return bookingRepository.save(booking);
    }

    public void cancelBooking(String bookingId) {
        // Logique pour "Annuler une réservation"
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    // Ajoute ces méthodes à ton BookingService actuel

    public List<Booking> getBookingsByUser(String userId) {
        return bookingRepository.findByUserId(userId);
    }

    public List<Booking> getBookingsByProperty(String propertyId) {
        return bookingRepository.findByPropertyId(propertyId);
    }

    public Booking getBookingById(String id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée"));
    }

    public Booking confirmBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Réservation introuvable"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Seules les réservations en attente peuvent être confirmées");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(LocalDateTime.now());
        return bookingRepository.save(booking);
    }
}