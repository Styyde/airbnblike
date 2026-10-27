package com.example.demo.Enums;

public enum BookingStatus {
    REQUESTED,
    REJECTED,
    EXPIRED,
    PENDING,     // En attente de confirmation
    CONFIRMED,   // Confirmé par l'hôte
    CANCELLED,   // Annulé
    COMPLETED    // Séjour terminé
}
