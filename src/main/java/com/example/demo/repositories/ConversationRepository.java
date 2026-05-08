package com.example.demo.repositories;

import com.example.demo.entities.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    // ===== RECHERCHES DE BASE =====

    /**
     * Trouver une conversation entre deux utilisateurs spécifiques
     * Les IDs doivent être triés (user1Id = plus petit, user2Id = plus grand)
     */
    Optional<Conversation> findByUser1IdAndUser2Id(String user1Id, String user2Id);

    /**
     * Trouver toutes les conversations d'un utilisateur (non archivées)
     */
    @Query("{ $and: [ " +
            "{ $or: [ { 'user1Id': ?0 }, { 'user2Id': ?0 } ] }, " +
            "{ 'isArchived': false } " +
            "] }")
    List<Conversation> findActiveConversationsByUserId(String userId);

    /**
     * Trouver toutes les conversations d'un utilisateur (y compris archivées)
     */
    @Query("{ $or: [ { 'user1Id': ?0 }, { 'user2Id': ?0 } ] }")
    List<Conversation> findAllConversationsByUserId(String userId);

    /**
     * Trouver une conversation par son ID en vérifiant qu'un utilisateur y a accès
     */
    @Query("{ $and: [ { '_id': ?0 }, { $or: [ { 'user1Id': ?1 }, { 'user2Id': ?1 } ] } ] }")
    Optional<Conversation> findByIdAndUserId(String conversationId, String userId);


    // ===== CONVERSATIONS AVEC MESSAGES NON LUS =====

    /**
     * Trouver les conversations où l'utilisateur a des messages non lus
     */
    @Query("{ $or: [ " +
            "{ 'user1Id': ?0, 'unreadCountForUser1': { $gt: 0 } }, " +
            "{ 'user2Id': ?0, 'unreadCountForUser2': { $gt: 0 } } " +
            "] }")
    List<Conversation> findConversationsWithUnreadMessages(String userId);

    /**
     * Compter le nombre total de messages non lus pour un utilisateur
     */
    @Query(value = "{ $or: [ " +
            "{ 'user1Id': ?0, 'unreadCountForUser1': { $gt: 0 } }, " +
            "{ 'user2Id': ?0, 'unreadCountForUser2': { $gt: 0 } } " +
            "] }",
            count = true)
    long countConversationsWithUnreadMessages(String userId);

    /**
     * Récupérer le nombre total de messages non lus (somme des compteurs)
     */
    default int getTotalUnreadCount(String userId) {
        List<Conversation> conversations = findConversationsWithUnreadMessages(userId);
        return conversations.stream()
                .mapToInt(conv -> {
                    if (conv.getUser1Id().equals(userId)) {
                        return conv.getUnreadCountForUser1();
                    } else {
                        return conv.getUnreadCountForUser2();
                    }
                })
                .sum();
    }


    // ===== MISES À JOUR =====

    /**
     * Mettre à jour le dernier message d'une conversation
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'lastMessage': ?1, 'lastMessageAt': ?2, 'updatedAt': ?2 } }")
    void updateLastMessage(String conversationId, String lastMessage, LocalDateTime lastMessageAt);

    /**
     * Incrémenter le compteur de messages non lus pour l'utilisateur 1
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'unreadCountForUser1': 1 }, '$set': { 'updatedAt': ?1 } }")
    void incrementUnreadCountForUser1(String conversationId, LocalDateTime updatedAt);

    /**
     * Incrémenter le compteur de messages non lus pour l'utilisateur 2
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$inc': { 'unreadCountForUser2': 1 }, '$set': { 'updatedAt': ?1 } }")
    void incrementUnreadCountForUser2(String conversationId, LocalDateTime updatedAt);

    /**
     * Réinitialiser les messages non lus pour un utilisateur spécifique
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'unreadCountForUser1': 0, 'updatedAt': ?2 } }")
    void resetUnreadCountForUser1(String conversationId, LocalDateTime updatedAt);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'unreadCountForUser2': 0, 'updatedAt': ?2 } }")
    void resetUnreadCountForUser2(String conversationId, LocalDateTime updatedAt);

    /**
     * Mise à jour générique du compteur (choisit automatiquement le bon utilisateur)
     */
    default void incrementUnreadCount(String conversationId, String userId) {
        Conversation conversation = findById(conversationId).orElseThrow();
        if (conversation.getUser1Id().equals(userId)) {
            incrementUnreadCountForUser1(conversationId, LocalDateTime.now());
        } else {
            incrementUnreadCountForUser2(conversationId, LocalDateTime.now());
        }
    }

    default void resetUnreadCount(String conversationId, String userId) {
        Conversation conversation = findById(conversationId).orElseThrow();
        if (conversation.getUser1Id().equals(userId)) {
            resetUnreadCountForUser1(conversationId, LocalDateTime.now());
        } else {
            resetUnreadCountForUser2(conversationId, LocalDateTime.now());
        }
    }


    // ===== ARCHIVAGE =====

    /**
     * Archiver une conversation pour un utilisateur
     */
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'isArchived': ?1, 'updatedAt': ?2 } }")
    void archiveConversation(String conversationId, Boolean isArchived, LocalDateTime updatedAt);

    /**
     * Trouver les conversations archivées d'un utilisateur
     */
    @Query("{ $and: [ " +
            "{ $or: [ { 'user1Id': ?0 }, { 'user2Id': ?0 } ] }, " +
            "{ 'isArchived': true } " +
            "] }")
    List<Conversation> findArchivedConversationsByUserId(String userId);

    /**
     * Désarchiver une conversation
     */
    default void unarchiveConversation(String conversationId) {
        archiveConversation(conversationId, false, LocalDateTime.now());
    }


    // ===== RECHERCHES AVANCÉES =====

    /**
     * Trouver les conversations d'un utilisateur triées par date du dernier message (plus récent d'abord)
     */
    @Query(value = "{ $or: [ { 'user1Id': ?0 }, { 'user2Id': ?0 } ] }",
            sort = "{ 'lastMessageAt': -1 }")
    List<Conversation> findConversationsByUserIdOrderByLastMessageAtDesc(String userId);

    /**
     * Trouver les conversations actives (moins de 30 jours sans message)
     */
    @Query("{ $and: [ " +
            "{ $or: [ { 'user1Id': ?0 }, { 'user2Id': ?0 } ] }, " +
            "{ 'lastMessageAt': { $gte: ?1 } }, " +
            "{ 'isArchived': false } " +
            "] }")
    List<Conversation> findRecentActiveConversations(String userId, LocalDateTime thirtyDaysAgo);

    /**
     * Trouver les conversations liées à une réservation spécifique
     */
    List<Conversation> findByBookingId(String bookingId);

    /**
     * Trouver si une conversation existe entre deux utilisateurs
     */
    default boolean existsConversation(String user1Id, String user2Id) {
        String id1 = user1Id.compareTo(user2Id) < 0 ? user1Id : user2Id;
        String id2 = user1Id.compareTo(user2Id) < 0 ? user2Id : user1Id;
        return findByUser1IdAndUser2Id(id1, id2).isPresent();
    }


    // ===== STATISTIQUES =====

    /**
     * Compter le nombre total de conversations d'un utilisateur
     */
    @Query(value = "{ $or: [ { 'user1Id': ?0 }, { 'user2Id': ?0 } ] }", count = true)
    long countByUserId(String userId);

    /**
     * Compter le nombre de conversations actives (non archivées) d'un utilisateur
     */
    @Query(value = "{ $and: [ " +
            "{ $or: [ { 'user1Id': ?0 }, { 'user2Id': ?0 } ] }, " +
            "{ 'isArchived': false } " +
            "] }", count = true)
    long countActiveByUserId(String userId);

    /**
     * Trouver les conversations sans message (lastMessage = null)
     */
    @Query("{ $and: [ " +
            "{ $or: [ { 'user1Id': ?0 }, { 'user2Id': ?0 } ] }, " +
            "{ 'lastMessage': null } " +
            "] }")
    List<Conversation> findEmptyConversations(String userId);


    // ===== SUPPRESSION / NETTOYAGE =====

    /**
     * Supprimer toutes les conversations d'un utilisateur (soft delete)
     */
    default void deleteAllUserConversations(String userId) {
        List<Conversation> conversations = findAllConversationsByUserId(userId);
        deleteAll(conversations);
    }

    /**
     * Supprimer les conversations vides (sans message) de plus de X jours
     */
    @Query(value = "{ 'lastMessage': null, 'createdAt': { $lt: ?0 } }",
            delete = true)
    void deleteEmptyConversationsOlderThan(LocalDateTime date);
}