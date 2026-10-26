package com.example.demo.services;

import com.example.demo.exceptions.ApiException;
import java.time.LocalDate;

public final class StayDates {
    private StayDates() {}
    public static void validate(LocalDate start, LocalDate end) {
        if (start == null || end == null || !end.isAfter(start) || start.isBefore(LocalDate.now())) {
            throw ApiException.badRequest("Dates invalides : arrivée aujourd'hui ou après, départ après l'arrivée");
        }
        if (end.isAfter(start.plusYears(1))) throw ApiException.badRequest("Un séjour ne peut pas dépasser un an");
    }
}
