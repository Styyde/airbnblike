package com.example.demo.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "amenities")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Amenity {
    @Id
    private String id;

    private String name;        // ex: "Wi-Fi", "Piscine", "Climatisation"
    private String icon;        // Nom de l'icône (ex: "wifi", "pool") pour votre front-end Angular
    private String category;    // Catégorie pour trier (ex: "Essentiels", "Sécurité", "Luxe")
}