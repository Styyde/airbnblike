package com.example.demo.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import com.example.demo.Enums.PropertyStatus;
@Document(collection = "properties")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Property {
    @Id
    private String id;
    @jakarta.validation.constraints.NotBlank
    private String title;
    private String description;
    @jakarta.validation.constraints.NotNull
    private com.example.demo.Enums.PropertyType type;
    @jakarta.validation.constraints.NotBlank
    private String address;
    @jakarta.validation.constraints.NotBlank
    private String city;
    @jakarta.validation.constraints.NotBlank
    private String country;
    private Double latitude;
    private Double longitude;
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.DecimalMin("0.01")
    @jakarta.validation.constraints.Digits(integer = 10, fraction = 2)
    @org.springframework.data.mongodb.core.mapping.Field(targetType = org.springframework.data.mongodb.core.mapping.FieldType.DECIMAL128)
    private java.math.BigDecimal pricePerNight;
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Min(1)
    private Integer maxGuests;
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Min(0)
    private Integer bedrooms;
    private List<String> imageUrls;
    private Long calendarRevision;
    @org.springframework.data.annotation.Version
    private Long version;
    private PropertyStatus status;
    @org.springframework.data.mongodb.core.index.Indexed
    private String ownerId;
    private List<String> amenityIds; // Liste des IDs des équipements disponibles pour ce logement
}
