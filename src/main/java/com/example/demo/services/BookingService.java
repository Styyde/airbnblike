package com.example.demo.services;

import com.example.demo.Enums.BookingStatus;
import com.example.demo.entities.Booking;
import com.example.demo.dto.BookingRequestDTO;
import com.example.demo.repositories.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final PropertyService propertyService; // Pour vérifier la disponibilité

    // ===== MÉTHODES EXISTANTES =====

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

    // ===== NOUVELLES MÉTHODES POUR L'ADMIN =====

    /**
     * Compter le nombre total de réservations
     */
    public long countAllBookings() {
        return bookingRepository.count();
    }

    /**
     * Compter le nombre de réservations par statut
     */
    public long countBookingsByStatus(BookingStatus status) {
        return bookingRepository.countByStatus(status);
    }

    /**
     * Calculer le revenu total (somme de toutes les réservations confirmées et complétées)
     * Version utilisant l'agrégation MongoDB (plus performante)
     */
    public Double getTotalRevenue() {
        List<BookingStatus> statuses = List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED);
        Double total = bookingRepository.sumTotalPriceByStatuses(statuses);
        return total != null ? total : 0.0;
    }

    /**
     * Calculer le revenu total (version alternative sans agrégation)
     * À utiliser si sumTotalPriceByStatuses pose problème
     */
    public Double getTotalRevenueAlternative() {
        List<Booking> allBookings = bookingRepository.findAll();

        return allBookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED ||
                        booking.getStatus() == BookingStatus.COMPLETED)
                .mapToDouble(Booking::getTotalPrice)
                .sum();
    }

    /**
     * Calculer le revenu pour une période spécifique
     */
    public Double getRevenueBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        List<BookingStatus> statuses = List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED);
        List<Booking> bookings = bookingRepository.findByStatusInAndCreatedAtBetween(statuses, startDate, endDate);

        return bookings.stream()
                .mapToDouble(Booking::getTotalPrice)
                .sum();
    }

    /**
     * Calculer le revenu pour une période spécifique (version alternative)
     */
    public Double getRevenueBetweenDatesAlternative(LocalDateTime startDate, LocalDateTime endDate) {
        List<Booking> allBookings = bookingRepository.findAll();

        return allBookings.stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED ||
                        booking.getStatus() == BookingStatus.COMPLETED)
                .filter(booking -> booking.getCreatedAt() != null &&
                        !booking.getCreatedAt().isBefore(startDate) &&
                        !booking.getCreatedAt().isAfter(endDate))
                .mapToDouble(Booking::getTotalPrice)
                .sum();
    }

    /**
     * Compter les réservations par mois pour une année donnée
     */
    public Map<String, Long> getBookingsCountByMonth(int year) {
        Map<String, Long> monthlyCounts = new LinkedHashMap<>();

        // Initialiser les mois
        String[] months = {"JANVIER", "FEVRIER", "MARS", "AVRIL", "MAI", "JUIN",
                "JUILLET", "AOUT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DECEMBRE"};

        for (String month : months) {
            monthlyCounts.put(month, 0L);
        }

        // Récupérer toutes les réservations de l'année
        LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59);
        List<Booking> bookings = bookingRepository.findByCreatedAtBetween(startOfYear, endOfYear);

        // Compter par mois
        for (Booking booking : bookings) {
            if (booking.getCreatedAt() != null) {
                int monthIndex = booking.getCreatedAt().getMonthValue() - 1;
                String monthName = months[monthIndex];
                monthlyCounts.put(monthName, monthlyCounts.get(monthName) + 1);
            }
        }

        return monthlyCounts;
    }

    /**
     * Compter les réservations par mois avec le mois en nombre (1-12)
     */
    public Map<Integer, Long> getBookingsCountByMonthNumber(int year) {
        Map<Integer, Long> monthlyCounts = new HashMap<>();

        // Initialiser les compteurs pour chaque mois
        for (int month = 1; month <= 12; month++) {
            monthlyCounts.put(month, 0L);
        }

        // Récupérer toutes les réservations de l'année
        LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59);
        List<Booking> bookings = bookingRepository.findByCreatedAtBetween(startOfYear, endOfYear);

        // Compter par mois
        for (Booking booking : bookings) {
            if (booking.getCreatedAt() != null) {
                int month = booking.getCreatedAt().getMonthValue();
                monthlyCounts.put(month, monthlyCounts.get(month) + 1);
            }
        }

        return monthlyCounts;
    }

    /**
     * Obtenir les statistiques détaillées pour l'admin
     */
    public Map<String, Object> getDetailedStats() {
        Map<String, Object> stats = new HashMap<>();

        // Réservations par statut
        stats.put("pending", bookingRepository.countByStatus(BookingStatus.PENDING));
        stats.put("confirmed", bookingRepository.countByStatus(BookingStatus.CONFIRMED));
        stats.put("cancelled", bookingRepository.countByStatus(BookingStatus.CANCELLED));
        stats.put("completed", bookingRepository.countByStatus(BookingStatus.COMPLETED));

        // Revenu total
        stats.put("totalRevenue", getTotalRevenue());

        // Nombre total
        stats.put("total", bookingRepository.count());

        // Revenu par statut
        double revenueFromConfirmed = getRevenueByStatus(BookingStatus.CONFIRMED);
        double revenueFromCompleted = getRevenueByStatus(BookingStatus.COMPLETED);
        stats.put("revenueFromConfirmed", revenueFromConfirmed);
        stats.put("revenueFromCompleted", revenueFromCompleted);

        return stats;
    }

    /**
     * Calculer le revenu pour un statut spécifique
     */
    public Double getRevenueByStatus(BookingStatus status) {
        List<Booking> bookings = bookingRepository.findByStatus(status);
        return bookings.stream()
                .mapToDouble(Booking::getTotalPrice)
                .sum();
    }

    /**
     * Trouver les réservations par période de dates (check-in/check-out)
     */
    public List<Booking> getBookingsByDateRange(LocalDate startDate, LocalDate endDate) {
        return bookingRepository.findBookingsInPeriod(startDate, endDate);
    }

    /**
     * Compter les réservations actives pour une propriété
     */
    public long countActiveBookingsByProperty(String propertyId, LocalDate currentDate) {
        return bookingRepository.findActiveBookings(propertyId, currentDate).size();
    }
}