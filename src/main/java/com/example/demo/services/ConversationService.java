package com.example.demo.services;

import com.example.demo.entities.Conversation;
import com.example.demo.entities.Message;
import com.example.demo.entities.User;
import com.example.demo.Enums.MessageStatus;
import com.example.demo.dto.ConversationDTO;
import com.example.demo.dto.MessageDTO;
import com.example.demo.repositories.ConversationRepository;
import com.example.demo.repositories.MessageRepository;
import com.example.demo.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    // ===== GESTION DES CONVERSATIONS =====

    /**
     * Créer ou récupérer une conversation existante entre deux utilisateurs
     */
    @Transactional
    public Conversation getOrCreateConversation(String user1Id, String user2Id, String bookingId) {
        // Vérifier que les utilisateurs existent
        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new RuntimeException("Utilisateur 1 non trouvé"));
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new RuntimeException("Utilisateur 2 non trouvé"));

        // Trier les IDs pour avoir des conversations uniques
        String id1 = user1Id.compareTo(user2Id) < 0 ? user1Id : user2Id;
        String id2 = user1Id.compareTo(user2Id) < 0 ? user2Id : user1Id;

        return conversationRepository.findByUser1IdAndUser2Id(id1, id2)
                .orElseGet(() -> {
                    Conversation conversation = Conversation.builder()
                            .user1Id(id1)
                            .user2Id(id2)
                            .bookingId(bookingId)
                            .isArchived(false)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .unreadCountForUser1(0)
                            .unreadCountForUser2(0)
                            .lastMessage(null)
                            .lastMessageAt(null)
                            .build();
                    log.info("Nouvelle conversation créée entre {} et {}", user1Id, user2Id);
                    return conversationRepository.save(conversation);
                });
    }

    /**
     * Récupérer toutes les conversations d'un utilisateur
     */
    public List<ConversationDTO> getUserConversations(String userId) {
        // Vérifier que l'utilisateur existe
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<Conversation> conversations = conversationRepository
                .findActiveConversationsByUserId(userId);

        return conversations.stream()
                .map(conv -> convertToDTO(conv, userId))
                .sorted(Comparator.comparing(ConversationDTO::getLastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Récupérer une conversation par son ID avec vérification d'accès
     */
    public Conversation getConversationById(String conversationId, String userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));

        // Vérifier que l'utilisateur fait partie de la conversation
        if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
            throw new RuntimeException("Accès non autorisé à cette conversation");
        }

        return conversation;
    }

    // ===== GESTION DES MESSAGES =====

    /**
     * Envoyer un message avec validation des participants
     */
    @Transactional
    public Message sendMessage(String conversationId, String senderId, String receiverId, String content) {
        // 1. Valider le contenu
        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Le message ne peut pas être vide");
        }
        if (content.length() > 5000) {
            throw new RuntimeException("Le message est trop long (maximum 5000 caractères)");
        }

        // 2. Récupérer et valider la conversation
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation non trouvée"));

        // 3. VALIDATION CRITIQUE : vérifier que l'expéditeur et le destinataire correspondent à la conversation
        validateConversationParticipants(conversation, senderId, receiverId);

        // 4. Vérifier que les utilisateurs existent
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Expéditeur non trouvé"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Destinataire non trouvé"));

        // 5. Créer le message
        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .timestamp(LocalDateTime.now())
                .isRead(false)
                .isDeletedForSender(false)
                .isDeletedForReceiver(false)
                .build();

        message = messageRepository.save(message);

        // 6. Mettre à jour la conversation
        updateConversationAfterMessage(conversation, senderId, content);

        log.info("Message envoyé dans la conversation {} de {} à {}",
                conversationId, senderId, receiverId);

        return message;
    }

    /**
     * Valider que l'expéditeur et le destinataire sont bien les deux participants de la conversation
     */
    private void validateConversationParticipants(Conversation conversation, String senderId, String receiverId) {
        String user1Id = conversation.getUser1Id();
        String user2Id = conversation.getUser2Id();

        // Vérifier que l'expéditeur est l'un des participants
        boolean isSenderValid = senderId.equals(user1Id) || senderId.equals(user2Id);
        if (!isSenderValid) {
            throw new RuntimeException("L'expéditeur ne fait pas partie de cette conversation");
        }

        // Vérifier que le destinataire est l'autre participant
        boolean isReceiverValid = (senderId.equals(user1Id) && receiverId.equals(user2Id)) ||
                (senderId.equals(user2Id) && receiverId.equals(user1Id));

        if (!isReceiverValid) {
            throw new RuntimeException("Le destinataire ne correspond pas à l'autre participant de la conversation");
        }
    }

    /**
     * Mettre à jour la conversation après l'envoi d'un message
     */
    private void updateConversationAfterMessage(Conversation conversation, String senderId, String content) {
        // Mettre à jour le dernier message
        conversation.setLastMessage(content.length() > 100 ? content.substring(0, 100) + "..." : content);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());

        // Incrémenter le compteur de non-lus pour le destinataire
        if (senderId.equals(conversation.getUser1Id())) {
            conversation.setUnreadCountForUser2(conversation.getUnreadCountForUser2() + 1);
        } else {
            conversation.setUnreadCountForUser1(conversation.getUnreadCountForUser1() + 1);
        }

        // Désarchiver si besoin
        if (conversation.getIsArchived()) {
            conversation.setIsArchived(false);
        }

        conversationRepository.save(conversation);
    }

    // ===== LECTURE DES MESSAGES =====

    /**
     * Récupérer tous les messages d'une conversation
     */
    public List<Message> getConversationMessages(String conversationId, String userId) {
        // Vérifier l'accès à la conversation
        Conversation conversation = getConversationById(conversationId, userId);

        // Récupérer les messages non supprimés pour cet utilisateur
        List<Message> messages = messageRepository.findByConversationId(conversationId)
                .stream()
                .filter(msg -> !isMessageDeletedForUser(msg, userId))
                .sorted(Comparator.comparing(Message::getTimestamp))
                .collect(Collectors.toList());

        return messages;
    }

    /**
     * Marquer tous les messages d'une conversation comme lus
     */
    @Transactional
    public int markMessagesAsRead(String conversationId, String userId) {
        Conversation conversation = getConversationById(conversationId, userId);

        // Récupérer les messages non lus destinés à cet utilisateur
        List<Message> unreadMessages = messageRepository.findByConversationIdAndReceiverIdAndIsReadFalse(
                conversationId, userId);

        // Marquer comme lus
        unreadMessages.forEach(msg -> msg.setIsRead(true));
        messageRepository.saveAll(unreadMessages);

        // Réinitialiser le compteur de non-lus dans la conversation
        if (userId.equals(conversation.getUser1Id())) {
            conversation.setUnreadCountForUser1(0);
        } else {
            conversation.setUnreadCountForUser2(0);
        }
        conversationRepository.save(conversation);

        log.info("{} messages marqués comme lus dans la conversation {} pour l'utilisateur {}",
                unreadMessages.size(), conversationId, userId);

        return unreadMessages.size();
    }

    /**
     * Compter les messages non lus pour un utilisateur
     */
    public int countUnreadMessages(String userId) {
        return conversationRepository.getTotalUnreadCount(userId);
    }

    // ===== SUPPRESSION DE MESSAGES =====

    /**
     * Supprimer un message (soft delete - marqué comme supprimé pour l'utilisateur)
     */
    @Transactional
    public void deleteMessageForUser(String messageId, String userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message non trouvé"));

        // Vérifier que l'utilisateur est l'expéditeur ou le destinataire
        if (!message.getSenderId().equals(userId) && !message.getReceiverId().equals(userId)) {
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer ce message");
        }

        // Soft delete : marquer comme supprimé pour cet utilisateur
        if (message.getSenderId().equals(userId)) {
            message.setIsDeletedForSender(true);
        }
        if (message.getReceiverId().equals(userId)) {
            message.setIsDeletedForReceiver(true);
        }

        messageRepository.save(message);
        log.info("Message {} supprimé pour l'utilisateur {}", messageId, userId);
    }

    /**
     * Supprimer un message pour tout le monde (admin ou propriétaire du message)
     */
    @Transactional
    public void deleteMessageForEveryone(String messageId, String userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message non trouvé"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Seul l'expéditeur ou un admin peut supprimer pour tout le monde
        boolean isAdmin = user.getRoles().contains("ROLE_ADMIN");
        if (!message.getSenderId().equals(userId) && !isAdmin) {
            throw new RuntimeException("Seul l'expéditeur ou un administrateur peut supprimer ce message");
        }

        // Hard delete
        messageRepository.delete(message);
        log.info("Message {} supprimé définitivement par {}", messageId, userId);

        // Mettre à jour le dernier message de la conversation si nécessaire
        updateLastMessageAfterDeletion(message.getConversationId());
    }

    /**
     * Mettre à jour le dernier message de la conversation après suppression
     */
    private void updateLastMessageAfterDeletion(String conversationId) {
        List<Message> remainingMessages = messageRepository.findByConversationId(conversationId);

        if (remainingMessages.isEmpty()) {
            conversationRepository.updateLastMessage(conversationId, null, null);
        } else {
            Message lastMessage = remainingMessages.stream()
                    .max(Comparator.comparing(Message::getTimestamp))
                    .orElse(null);

            if (lastMessage != null) {
                String preview = lastMessage.getContent().length() > 100 ?
                        lastMessage.getContent().substring(0, 100) + "..." :
                        lastMessage.getContent();
                conversationRepository.updateLastMessage(conversationId, preview, lastMessage.getTimestamp());
            }
        }
    }

    /**
     * Supprimer tous les messages d'une conversation (pour un utilisateur)
     */
    @Transactional
    public void clearConversationForUser(String conversationId, String userId) {
        Conversation conversation = getConversationById(conversationId, userId);

        List<Message> messages = messageRepository.findByConversationId(conversationId);

        for (Message message : messages) {
            if (userId.equals(message.getSenderId())) {
                message.setIsDeletedForSender(true);
            }
            if (userId.equals(message.getReceiverId())) {
                message.setIsDeletedForReceiver(true);
            }
        }

        messageRepository.saveAll(messages);

        // Réinitialiser les compteurs pour cet utilisateur
        if (userId.equals(conversation.getUser1Id())) {
            conversation.setUnreadCountForUser1(0);
            conversation.setLastMessage(null);
            conversation.setLastMessageAt(null);
        } else {
            conversation.setUnreadCountForUser2(0);
            conversation.setLastMessage(null);
            conversation.setLastMessageAt(null);
        }

        conversationRepository.save(conversation);
        log.info("Conversation {} nettoyée pour l'utilisateur {}", conversationId, userId);
    }

    // ===== ARCHIVAGE DES CONVERSATIONS =====

    /**
     * Archiver une conversation
     */
    @Transactional
    public void archiveConversation(String conversationId, String userId) {
        Conversation conversation = getConversationById(conversationId, userId);
        conversation.setIsArchived(true);
        conversationRepository.save(conversation);
        log.info("Conversation {} archivée par {}", conversationId, userId);
    }

    /**
     * Désarchiver une conversation
     */
    @Transactional
    public void unarchiveConversation(String conversationId, String userId) {
        Conversation conversation = getConversationById(conversationId, userId);
        conversation.setIsArchived(false);
        conversationRepository.save(conversation);
        log.info("Conversation {} désarchivée par {}", conversationId, userId);
    }

    // ===== MÉTHODES UTILITAIRES =====

    /**
     * Vérifier si un message est supprimé pour un utilisateur
     */
    private boolean isMessageDeletedForUser(Message message, String userId) {
        if (message.getSenderId().equals(userId)) {
            return message.getIsDeletedForSender() != null && message.getIsDeletedForSender();
        }
        if (message.getReceiverId().equals(userId)) {
            return message.getIsDeletedForReceiver() != null && message.getIsDeletedForReceiver();
        }
        return false;
    }

    /**
     * Convertir une Conversation en DTO
     */
    private ConversationDTO convertToDTO(Conversation conversation, String currentUserId) {
        String otherUserId = conversation.getUser1Id().equals(currentUserId) ?
                conversation.getUser2Id() : conversation.getUser1Id();

        User otherUser = userRepository.findById(otherUserId).orElse(null);

        int unreadCount = conversation.getUser1Id().equals(currentUserId) ?
                conversation.getUnreadCountForUser1() : conversation.getUnreadCountForUser2();

        return ConversationDTO.builder()
                .conversationId(conversation.getId())
                .otherUserId(otherUserId)
                .otherUserName(otherUser != null ? otherUser.getUsername() : "Utilisateur inconnu")
                .otherUserAvatar(null) // À ajouter si tu as un champ avatar
                .lastMessage(conversation.getLastMessage())
                .lastMessageAt(conversation.getLastMessageAt())
                .unreadCount(unreadCount)
                .isArchived(conversation.getIsArchived())
                .bookingId(conversation.getBookingId())
                .build();
    }

    /**
     * Compter le nombre total de conversations pour un utilisateur
     */
    public long countUserConversations(String userId) {
        return conversationRepository.countByUserId(userId);
    }

    /**
     * Supprimer définitivement une conversation (admin seulement)
     */
    @Transactional
    public void deleteConversationPermanently(String conversationId, String adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        if (!admin.getRoles().contains("ROLE_ADMIN")) {
            throw new RuntimeException("Seul un administrateur peut supprimer définitivement une conversation");
        }

        // Supprimer tous les messages
        messageRepository.deleteByConversationId(conversationId);

        // Supprimer la conversation
        conversationRepository.deleteById(conversationId);

        log.info("Conversation {} supprimée définitivement par l'admin {}", conversationId, adminId);
    }
}