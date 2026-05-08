package com.example.demo.dto;

import com.example.demo.Enums.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {

    private String id;
    private String conversationId;

    // Expéditeur
    private String senderId;
    private String senderName;
    private String senderAvatar;

    // Destinataire
    private String receiverId;
    private String receiverName;
    private String receiverAvatar;

    // Contenu
    private String content;
    private String contentPreview;  // Version tronquée pour les listes

    // Statut
    private MessageStatus status;
    private Boolean isRead;
    private Boolean isDeletedForSender;
    private Boolean isDeletedForReceiver;

    // Dates
    private LocalDateTime timestamp;
    private String formattedDate;   // "15/01/2025 14:30"
    private String timeAgo;         // "il y a 5 minutes"

    // Contexte
    private String bookingId;

    // Pour l'interface (frontend)
    private Boolean isMine;          // Est-ce que c'est moi qui ai envoyé ?
    private String alignmentClass;   // "message-left" ou "message-right"
    private String statusIcon;       // ✅, ✓✓, ⏰

    // Réponse à un message (optionnel)
    private String replyToId;
    private String replyToContent;
    private String replyToSenderName;
}