package com.example.demo.services;

import com.example.demo.Enums.*;
import com.example.demo.entities.Booking;
import com.example.demo.entities.Payment;
import com.example.demo.exceptions.ApiException;
import com.example.demo.repositories.BookingRepository;
import com.example.demo.repositories.PaymentRepository;
import com.example.demo.security.ResourceAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentGatewayService paymentGatewayService;
    private final ResourceAccess access;
    private final PropertyCalendar calendar;

    @Transactional
    public Payment processPayment(String bookingId, PaymentMethod method) {
        Booking booking = access.bookingGuest(bookingId);
        calendar.lock(booking.getPropertyId());
        if (method == null) throw ApiException.badRequest("Méthode de paiement requise");
        var existing = paymentRepository.findByBookingId(bookingId);
        if (existing.isPresent() && existing.get().getStatus() == PaymentStatus.COMPLETED
                && booking.getStatus() == BookingStatus.CONFIRMED) return existing.get();
        if (booking.getStatus() != BookingStatus.PENDING || existing.isPresent()) {
            throw ApiException.conflict("Réservation non payable");
        }
        if (booking.getStartDate().isBefore(LocalDate.now())) throw ApiException.conflict("Réservation expirée");
        if (booking.getExpiresAt() != null && !booking.getExpiresAt().isAfter(LocalDateTime.now())) throw ApiException.conflict("Réservation expirée");
        paymentGatewayService.requireEnabled();
        Payment payment = Payment.builder().bookingId(bookingId).amount(booking.getTotalPrice())
                .method(method).status(PaymentStatus.PENDING).build();
        payment = paymentRepository.save(payment);
        payment.setGatewayTransactionId(paymentGatewayService.charge(payment.getAmount(), method, booking.getUserId()));
        payment.setStatus(PaymentStatus.COMPLETED);
        payment = paymentRepository.save(payment);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        return payment;
    }

    @Transactional
    public Payment refundPayment(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> ApiException.notFound("Paiement introuvable"));
        Booking booking = access.bookingParticipant(payment.getBookingId());
        calendar.lock(booking.getPropertyId());
        if (payment.getStatus() == PaymentStatus.REFUNDED) return payment;
        if (payment.getStatus() != PaymentStatus.COMPLETED || !booking.getStartDate().isAfter(LocalDate.now())) {
            throw ApiException.conflict("Seul un séjour payé et futur peut être remboursé");
        }
        paymentGatewayService.refund(payment.getGatewayTransactionId(), payment.getAmount());
        payment.setStatus(PaymentStatus.REFUNDED);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        return paymentRepository.save(payment);
    }

    public PaymentStatus getPaymentStatus(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> ApiException.notFound("Paiement introuvable"));
        access.bookingParticipant(payment.getBookingId());
        return payment.getStatus();
    }

    public Payment getPaymentByBookingId(String bookingId) {
        access.bookingParticipant(bookingId);
        return paymentRepository.findByBookingId(bookingId).orElseThrow(() -> ApiException.notFound("Paiement introuvable"));
    }
}
