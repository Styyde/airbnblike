package com.example.demo.entities;
import com.example.demo.Enums.BookingStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "bookings")
@org.springframework.data.mongodb.core.index.CompoundIndexes({
    @org.springframework.data.mongodb.core.index.CompoundIndex(name = "property_dates_status", def = "{'propertyId':1,'status':1,'startDate':1,'endDate':1}"),
    @org.springframework.data.mongodb.core.index.CompoundIndex(name = "pending_expiry", def = "{'status':1,'expiresAt':1}")
})
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
    @org.springframework.data.mongodb.core.mapping.Field(targetType = org.springframework.data.mongodb.core.mapping.FieldType.DECIMAL128)
    private java.math.BigDecimal totalPrice;

    @org.springframework.data.annotation.Version
    private Long version;
    private BookingStatus status;  // Utiliser l'enum

    @org.springframework.data.mongodb.core.index.Indexed
    private String userId;
    private String propertyId;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;  // Pour le suivi des modifications
    private java.time.Instant expiresAt;
    private String guestMessage;
    private String decisionReason;
}
