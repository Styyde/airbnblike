package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class PropertyDTO {
    private String id;
    private String title;
    private String description;
    private String type;
    private String city;
    private String country;
    private Double pricePerNight;
    private Integer maxGuests;

    // Au lieu d'une entité Photo, on utilise une liste de liens directs
    private List<String> imageUrls;

    // On peut renvoyer les noms des équipements au lieu de leurs IDs
    private List<String> amenities;

    private String ownerName; // Plus utile que l'ownerId pour le front
}