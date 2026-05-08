package com.example.demo.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    @Id
    private String id;

    private String conversationId;
    private String senderId;
    private String receiverId;
    private String content;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Builder.Default
    private Boolean isRead = false;

    // À AJOUTER : Soft delete flags
    @Builder.Default
    private Boolean isDeletedForSender = false;

    @Builder.Default
    private Boolean isDeletedForReceiver = false;

    private String bookingId;
}