package com.example.demo.Enums;

public enum PropertyStatus {
    AVAILABLE,    // Disponible à la location
    MAINTENANCE,  // En maintenance (indisponible)
    DELETED,      // Supprimé (soft delete)
    PENDING_VERIFICATION // En attente de vérification par admin
}