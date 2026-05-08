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
    private String title;
    private String description;
    private String type; // ex: Appartement, Maison, Villa
    private String address;
    private String city;
    private String country;
    private Double latitude;
    private Double longitude;
    private Double pricePerNight;
    private Integer maxGuests;
    private Integer bedrooms;
    private PropertyStatus status;
    private String ownerId; // Référence vers l'ID de l'User (Propriétaire)
    private List<String> amenityIds; // Liste des IDs des équipements disponibles pour ce logement
}