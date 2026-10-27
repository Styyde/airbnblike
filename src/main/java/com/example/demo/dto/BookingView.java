package com.example.demo.dto;
import com.example.demo.Enums.BookingStatus;
import java.math.BigDecimal;
import java.time.*;

public record BookingView(String id, String propertyId, String userId, LocalDate startDate, LocalDate endDate,
        Integer numberOfGuests, BigDecimal totalPrice, BookingStatus status, Instant expiresAt,
        LocalDateTime createdAt, String guestMessage, String decisionReason, String travelerName,
        String propertyTitle, String propertyCity, String propertyImage, String ownerId, String ownerName, String currency) {}
