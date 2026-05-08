package com.example.demo.entities;
import com.example.demo.Enums.BookingStatus;
import com.example.demo.Enums.BookingStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder  // Ajoute Builder pour faciliter la création
public class Booking {
    @Id
    private String id;

    private LocalDate startDate;
    private LocalDate endDate;

    private Integer numberOfGuests;
    private Double totalPrice;
    private BookingStatus status;  // Utiliser l'enum

    private String userId;
    private String propertyId;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;  // Pour le suivi des modifications
}