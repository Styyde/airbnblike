package com.example.demo.services;

import com.example.demo.Enums.BookingStatus;
import com.example.demo.Enums.PaymentMethod;
import com.example.demo.Enums.PaymentStatus;
import com.example.demo.entities.Booking;
import com.example.demo.entities.Payment;
import com.example.demo.repositories.BookingRepository;
import com.example.demo.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentGatewayService paymentGatewayService;

    /**
     * Traiter un paiement pour une réservation
     */
    public Payment processPayment(String bookingId, PaymentMethod method) {
        // 1. Récupérer la réservation
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Réservation non trouvée: " + bookingId));

        // 2. Vérifier que la réservation est en attente
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Seules les réservations en attente peuvent être payées");
        }

        // 3. Vérifier qu'il n'y a pas déjà un paiement
        if (paymentRepository.findByBookingId(bookingId).isPresent()) {
            throw new RuntimeException("Un paiement existe déjà pour cette réservation");
        }

        // 4. Créer l'objet Payment avec Builder
        Payment payment = Payment.builder()
                .bookingId(bookingId)
                .amount(booking.getTotalPrice())
                .method(method)
                .status(PaymentStatus.PENDING)
                .paymentDate(LocalDateTime.now())
                .build();

        // Sauvegarder avec status PENDING
        payment = paymentRepository.save(payment);

        try {
            // 5. Appeler la passerelle de paiement externe
            paymentGatewayService.charge(
                    booking.getTotalPrice(),
                    method,
                    booking.getUserId()
            );

            // 6. Mettre à jour le paiement avec succès
            payment.setStatus(PaymentStatus.COMPLETED);
            payment = paymentRepository.save(payment);

            // 7. Confirmer la réservation
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);

            log.info("Paiement réussi pour la réservation {}", bookingId);

        } catch (Exception e) {
            // 8. En cas d'échec, marquer le paiement comme échoué
            log.error("Échec du paiement pour la réservation {}: {}", bookingId, e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            // 9. Annuler la réservation
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);

            throw new RuntimeException("Le paiement a échoué: " + e.getMessage());
        }

        return payment;
    }

    /**
     * Rembourser un paiement
     */
    public Payment refundPayment(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé: " + paymentId));

        // Comparer avec l'enum correctement
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new RuntimeException("Seuls les paiements complétés peuvent être remboursés");
        }

        try {
            // Appeler la passerelle pour rembourser
            paymentGatewayService.refund(paymentId, payment.getAmount());

            payment.setStatus(PaymentStatus.REFUNDED);
            payment = paymentRepository.save(payment);

            // Annuler la réservation associée
            Booking booking = bookingRepository.findById(payment.getBookingId()).orElseThrow();
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);

            log.info("Remboursement effectué pour le paiement {}", paymentId);

        } catch (Exception e) {
            log.error("Échec du remboursement pour {}: {}", paymentId, e.getMessage());
            throw new RuntimeException("Le remboursement a échoué: " + e.getMessage());
        }

        return payment;
    }

    /**
     * Vérifier le statut d'un paiement
     */
    public PaymentStatus getPaymentStatus(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Paiement non trouvé"));
        return payment.getStatus();  // Retourne directement l'enum
    }

    /**
     * Récupérer le paiement d'une réservation
     */
    public Payment getPaymentByBookingId(String bookingId) {
        return paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Aucun paiement trouvé pour cette réservation"));
    }
}