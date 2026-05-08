package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {

    private String conversationId;

    // Informations sur l'autre participant
    private String otherUserId;
    private String otherUserName;
    private String otherUserAvatar;
    private String otherUserEmail;

    // Dernier message
    private String lastMessage;
    private LocalDateTime lastMessageAt;

    // Statistiques
    private Integer unreadCount;
    private Integer totalMessages;

    // Statut de la conversation
    private Boolean isArchived;
    private Boolean isBlocked;

    // Contexte
    private String bookingId;
    private String propertyId;
    private String propertyTitle;

    // Dates
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Pour l'affichage (frontend)
    private String lastMessagePreview;
    private String timeAgo;  // "il y a 5 minutes", "hier", etc.
}