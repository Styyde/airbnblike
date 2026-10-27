package com.example.demo.services;

import com.example.demo.Enums.*;
import com.example.demo.entities.Booking;
import com.example.demo.dto.BookingRequestDTO;
import com.example.demo.repositories.*;
import com.example.demo.security.ResourceAccess;
import com.example.demo.exceptions.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final BookingRepository bookingRepository;
    private final PropertyService propertyService;
    private final PropertyCalendar calendar;
    private final ResourceAccess access;
    private final PaymentRepository paymentRepository;
    private final PropertyRepository properties;

    @Transactional
    public Booking createBooking(BookingRequestDTO dto) {
        StayDates.validate(dto.getStartDate(), dto.getEndDate());
        calendar.lock(dto.getPropertyId());
        var property = propertyService.getPropertyById(dto.getPropertyId());
        String guest = access.current().getId();
        if (property.getOwnerId().equals(guest)) throw ApiException.badRequest("Vous ne pouvez pas réserver votre propre logement.");
        if (dto.getNumberOfGuests() == null || dto.getNumberOfGuests() < 1 || dto.getNumberOfGuests() > property.getMaxGuests())
            throw ApiException.badRequest("Le nombre de voyageurs dépasse la capacité du logement.");
        if (!propertyService.isAvailable(property.getId(), dto.getStartDate(), dto.getEndDate()))
            throw ApiException.conflict("Ces dates ne sont plus disponibles.");
        boolean duplicate = bookingRepository.findByUserId(guest).stream().anyMatch(b ->
                b.getPropertyId().equals(property.getId()) && b.getStartDate().equals(dto.getStartDate()) && b.getEndDate().equals(dto.getEndDate())
                && List.of(BookingStatus.REQUESTED, BookingStatus.PENDING, BookingStatus.CONFIRMED).contains(b.getStatus())
                && (b.getStatus() == BookingStatus.CONFIRMED || b.getExpiresAt() == null || b.getExpiresAt().isAfter(Instant.now())));
        if (duplicate) throw ApiException.conflict("Vous avez déjà une demande pour ces dates.");
        return bookingRepository.save(Booking.builder().userId(guest).propertyId(property.getId())
                .startDate(dto.getStartDate()).endDate(dto.getEndDate()).numberOfGuests(dto.getNumberOfGuests())
                .totalPrice(propertyService.calculateTotalPrice(property.getId(), dto.getStartDate(), dto.getEndDate()))
                .guestMessage(dto.getGuestMessage()).status(BookingStatus.REQUESTED)
                .expiresAt(Instant.now().plus(Duration.ofHours(48))).createdAt(LocalDateTime.now()).build());
    }

    @Transactional
    public Booking accept(String id) {
        Booking booking = getBookingById(id);
        access.propertyOwner(booking.getPropertyId());
        calendar.lock(booking.getPropertyId());
        if (booking.getStatus() == BookingStatus.PENDING && !expired(booking)) return booking;
        if (booking.getStatus() != BookingStatus.REQUESTED || expired(booking)) throw ApiException.conflict("Cette demande ne peut plus être acceptée.");
        if (!propertyService.isAvailable(booking.getPropertyId(), booking.getStartDate(), booking.getEndDate()))
            throw ApiException.conflict("Ces dates ne sont plus disponibles.");
        booking.setStatus(BookingStatus.PENDING);
        booking.setExpiresAt(Instant.now().plus(Duration.ofMinutes(30)));
        booking.setUpdatedAt(LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    @Transactional
    public Booking reject(String id, String reason) {
        Booking booking = getBookingById(id);
        access.propertyOwner(booking.getPropertyId());
        calendar.lock(booking.getPropertyId());
        if (booking.getStatus() == BookingStatus.REJECTED) return booking;
        if (booking.getStatus() != BookingStatus.REQUESTED || expired(booking)) throw ApiException.conflict("Cette demande ne peut plus être refusée.");
        booking.setStatus(BookingStatus.REJECTED);
        booking.setDecisionReason(reason == null || reason.isBlank() ? "L’hôte n’a pas pu accepter votre demande." : reason.strip());
        booking.setUpdatedAt(LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    private boolean expired(Booking booking) {
        return booking.getStartDate().isBefore(LocalDate.now()) || booking.getExpiresAt() != null && !booking.getExpiresAt().isAfter(Instant.now());
    }

    @Transactional
    public void cancelBooking(String id) {
        Booking booking = access.bookingGuest(id);
        calendar.lock(booking.getPropertyId());
        if (booking.getStatus() == BookingStatus.CANCELLED) return;
        if (!List.of(BookingStatus.REQUESTED, BookingStatus.PENDING).contains(booking.getStatus()) || expired(booking))
            throw ApiException.conflict("Seule une demande active et impayée peut être annulée.");
        if (paymentRepository.findByBookingId(id).filter(p -> p.getStatus() == PaymentStatus.COMPLETED).isPresent())
            throw ApiException.conflict("Utilisez le remboursement pour annuler un séjour payé.");
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);
    }

    @Transactional
    public Booking completeBooking(String id) {
        Booking booking = getBookingById(id);
        access.propertyOwner(booking.getPropertyId());
        calendar.lock(booking.getPropertyId());
        if (booking.getStatus() == BookingStatus.COMPLETED) return booking;
        if (booking.getStatus() != BookingStatus.CONFIRMED || booking.getEndDate().isAfter(LocalDate.now()))
            throw ApiException.conflict("Seul un séjour confirmé et terminé peut être clôturé.");
        booking.setStatus(BookingStatus.COMPLETED);
        booking.setUpdatedAt(LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    @Transactional
    public void expireBooking(String id) {
        Booking booking = getBookingById(id);
        calendar.lock(booking.getPropertyId());
        if (List.of(BookingStatus.REQUESTED, BookingStatus.PENDING).contains(booking.getStatus()) && expired(booking)) {
            booking.setStatus(BookingStatus.EXPIRED);
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);
        }
    }

    public Booking getBookingById(String id) { return bookingRepository.findById(id).orElseThrow(() -> ApiException.notFound("Réservation introuvable.")); }
    public List<Booking> getBookingsByUser(String id) { return bookingRepository.findByUserId(id); }
    public List<Booking> getBookingsByProperty(String id) { return bookingRepository.findByPropertyId(id); }
    public List<Booking> getHostBookings() { return bookingRepository.findByPropertyIdIn(properties.findByOwnerId(access.current().getId()).stream().map(p -> p.getId()).toList()); }
    public long countAllBookings() { return bookingRepository.count(); }
    public long countBookingsByStatus(BookingStatus status) { return bookingRepository.countByStatus(status); }
    public BigDecimal getTotalRevenue() {
        return bookingRepository.sumTotalPriceByStatuses(List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED)).stream()
                .map(BookingRepository.RevenueTotal::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
