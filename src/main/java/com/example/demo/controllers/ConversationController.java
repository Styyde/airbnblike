package com.example.demo.controllers;

import com.example.demo.dto.ConversationDTO;
import com.example.demo.dto.MessageDTO;
import com.example.demo.dto.SendMessageRequestDTO;
import com.example.demo.entities.Conversation;
import com.example.demo.entities.Message;
import com.example.demo.services.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ConversationDTO>> getUserConversations(@PathVariable String userId) {
        return ResponseEntity.ok(conversationService.getUserConversations(userId));
    }

    @PostMapping
    public ResponseEntity<Conversation> getOrCreateConversation(
            @RequestParam String user1Id,
            @RequestParam String user2Id,
            @RequestParam(required = false) String bookingId) {
        return ResponseEntity.ok(conversationService.getOrCreateConversation(user1Id, user2Id, bookingId));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<Message>> getMessages(
            @PathVariable String conversationId,
            @RequestParam String userId) {
        return ResponseEntity.ok(conversationService.getConversationMessages(conversationId, userId));
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<Message> sendMessage(
            @PathVariable String conversationId,
            @RequestBody SendMessageRequestDTO request) {
        return ResponseEntity.ok(conversationService.sendMessage(
                conversationId,
                request.getSenderId(),
                request.getReceiverId(),
                request.getContent()
        ));
    }

    @PutMapping("/{conversationId}/read")
    public ResponseEntity<Integer> markAsRead(
            @PathVariable String conversationId,
            @RequestParam String userId) {
        return ResponseEntity.ok(conversationService.markMessagesAsRead(conversationId, userId));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable String messageId,
            @RequestParam String userId) {
        conversationService.deleteMessageForUser(messageId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{conversationId}/archive")
    public ResponseEntity<Void> archiveConversation(
            @PathVariable String conversationId,
            @RequestParam String userId) {
        conversationService.archiveConversation(conversationId, userId);
        return ResponseEntity.noContent().build();
    }
}