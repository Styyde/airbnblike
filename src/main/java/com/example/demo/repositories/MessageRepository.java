package com.example.demo.repositories;

import com.example.demo.entities.Message;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    // ===== RECHERCHES DE BASE =====

    // CORRIGÉ : findByConversationId (pas findByConversationald)
    // CORRIGÉ : findByConversationIdOrderByTimestampAsc
    List<Message> findByConversationIdOrderByTimestampAsc(String conversationId);

    // CORRIGÉ : findByConversationIdOrderByTimestampDesc
    List<Message> findByConversationIdOrderByTimestampDesc(String conversationId);

    // CORRIGÉ : findByConversationIdAndReceiverIdAndIsReadFalse
    List<Message> findByConversationIdAndReceiverIdAndIsReadFalse(String conversationId, String receiverId);

    // ===== RECHERCHES AVEC FILTRES DE SUPPRESSION =====

    /**
     * Trouver les messages visibles pour un utilisateur (non supprimés pour lui)
     */
    @Query("{ 'conversationId': ?0, " +
            "'$or': [ " +
            "{ 'senderId': ?1, 'isDeletedForSender': false }, " +
            "{ 'receiverId': ?1, 'isDeletedForReceiver': false } " +
            "] }")
    List<Message> findVisibleMessagesByConversationIdAndUserId(String conversationId, String userId);

    /**
     * Trouver tous les messages d'une conversation (y compris supprimés)
     */
    List<Message> findByConversationId(String conversationId);

    // ===== RECHERCHES PAR STATUT =====

    /**
     * Trouver les messages non lus d'un destinataire
     */
    List<Message> findByReceiverIdAndIsReadFalse(String receiverId);

    /**
     * Compter les messages non lus d'un destinataire
     */
    long countByReceiverIdAndIsReadFalse(String receiverId);

    /**
     * Compter les messages dans une conversation
     */
    long countByConversationId(String conversationId);

    /**
     * Vérifier si un message existe dans une conversation
     */
    boolean existsByConversationId(String conversationId);

    // ===== SUPPRESSION =====

    /**
     * Supprimer tous les messages d'une conversation (hard delete)
     * CORRIGÉ : deleteByConversationId (pas deleteByConversationald)
     */
    void deleteByConversationId(String conversationId);

    /**
     * Supprimer un message (hard delete)
     */
    void deleteById(String id);

    // ===== RECHERCHES AVANCÉES =====

    /**
     * Trouver le dernier message d'une conversation
     */
    Optional<Message> findFirstByConversationIdOrderByTimestampDesc(String conversationId);

    /**
     * Trouver les messages après une certaine date
     */
    List<Message> findByConversationIdAndTimestampAfter(String conversationId, LocalDateTime timestamp);

    /**
     * Marquer tous les messages d'une conversation comme lus pour un destinataire
     */
    @Query("{ 'conversationId': ?0, 'receiverId': ?1, 'isRead': false }")
    @org.springframework.data.mongodb.repository.Update("{ '$set': { 'isRead': true } }")
    void markAllAsReadByConversationIdAndReceiverId(String conversationId, String receiverId);
}
