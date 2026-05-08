package com.example.demo.Enums;

public enum PaymentStatus {
    PENDING,      // En attente de paiement
    COMPLETED,    // Paiement réussi
    FAILED,       // Échec du paiement
    REFUNDED,     // Remboursé
    CANCELLED     // Annulé avant paiement
}