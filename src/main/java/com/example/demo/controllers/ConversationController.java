package com.example.demo.controllers;

import com.example.demo.dto.*;
import com.example.demo.entities.*;
import com.example.demo.security.ResourceAccess;
import com.example.demo.services.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService conversationService;
    private final ResourceAccess access;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ConversationDTO>> getUserConversations(@PathVariable String userId) {
        access.self(userId);
        return ResponseEntity.ok(conversationService.getUserConversations(userId));
    }

    @PostMapping
    public ResponseEntity<Conversation> getOrCreateConversation(@RequestParam String user1Id,
            @RequestParam String user2Id, @RequestParam(required = false) String bookingId) {
        access.self(user1Id);
        return ResponseEntity.ok(conversationService.getOrCreateConversation(user1Id, user2Id, bookingId));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<Message>> getMessages(@PathVariable String conversationId) {
        return ResponseEntity.ok(conversationService.getConversationMessages(conversationId, access.current().getId()));
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<Message> sendMessage(@PathVariable String conversationId, @Valid @RequestBody SendMessageRequestDTO request) {
        return ResponseEntity.ok(conversationService.sendMessage(conversationId, access.current().getId(), request.getReceiverId(), request.getContent()));
    }

    @PutMapping("/{conversationId}/read")
    public ResponseEntity<Integer> markAsRead(@PathVariable String conversationId) {
        return ResponseEntity.ok(conversationService.markMessagesAsRead(conversationId, access.current().getId()));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable String messageId) {
        conversationService.deleteMessageForUser(messageId, access.current().getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{conversationId}/archive")
    public ResponseEntity<Void> archiveConversation(@PathVariable String conversationId) {
        conversationService.archiveConversation(conversationId, access.current().getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{conversationId}/unarchive")
    public ResponseEntity<Void> unarchiveConversation(@PathVariable String conversationId) {
        conversationService.unarchiveConversation(conversationId, access.current().getId());
        return ResponseEntity.noContent().build();
    }
}
