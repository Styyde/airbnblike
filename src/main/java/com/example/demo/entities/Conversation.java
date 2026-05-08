package com.example.demo.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "conversations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {
    @Id
    private String id;

    // Les deux participants (ordre alphabétique pour unicité)
    private String user1Id;  // Le plus petit ID alphabétique
    private String user2Id;  // Le plus grand ID alphabétique

    // Métadonnées
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Integer unreadCountForUser1;
    private Integer unreadCountForUser2;

    // Référence optionnelle à une réservation
    private String bookingId;

    // Statut
    private Boolean isArchived;  // Archivé par l'un des users
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}