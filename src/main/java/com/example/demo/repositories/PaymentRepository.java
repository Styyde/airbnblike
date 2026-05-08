package com.example.demo.repositories;

import com.example.demo.Enums.PaymentStatus;
import com.example.demo.entities.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    // Trouver le paiement d'une réservation
    Optional<Payment> findByBookingId(String bookingId);

    // Trouver par statut
    List<Payment> findByStatus(PaymentStatus status);

    // Trouver les paiements échoués récents
    @Query("{ 'status': 'FAILED', 'paymentDate': { $gte: ?0 } }")
    List<Payment> findFailedPaymentsSince(LocalDateTime since);

    // Trouver les paiements par méthode
    List<Payment> findByMethod(String method);

    // Calculer le montant total des paiements complétés pour une période
    @Query(value = "{ 'status': 'COMPLETED', 'paymentDate': { $gte: ?0, $lte: ?1 } }",
            fields = "{ 'amount': 1 }")
    List<Payment> findCompletedPaymentsInPeriod(LocalDateTime startDate, LocalDateTime endDate);

    // Trouver les paiements à rembourser (complétés mais réservation annulée)
    @Query("{ 'status': 'COMPLETED', 'bookingId': { $in: ?0 } }")
    List<Payment> findPaymentsToRefund(List<String> cancelledBookingIds);

    // Compter les paiements par statut
    @Query(value = "{ 'status': ?0 }", count = true)
    long countByStatus(PaymentStatus status);

    // Trouver les paiements récents d'un utilisateur via ses réservations
    @Query("{ 'bookingId': { $in: ?0 }, 'status': 'COMPLETED' }")
    List<Payment> findCompletedPaymentsByBookingIds(List<String> bookingIds);
}