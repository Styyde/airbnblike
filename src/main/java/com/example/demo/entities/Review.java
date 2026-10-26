package com.example.demo.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {
    @Id
    private String id;

    private String authorId;    // ID de l'User qui laisse l'avis (le voyageur)
    private String propertyId;  // ID de la Property concernée
    @org.springframework.data.mongodb.core.index.Indexed(unique = true)
    private String bookingId;

    private Integer rating;     // Note globale (ex: de 1 à 5)
    private String comment;     // Le texte du commentaire

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
