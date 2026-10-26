package com.example.demo.repositories;

import com.example.demo.entities.Conversation;
import org.springframework.data.mongodb.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
    Optional<Conversation> findByUser1IdAndUser2Id(String user1Id, String user2Id);

    @Query("{ $or: [ { 'user1Id': ?0 }, { 'user2Id': ?0 } ] }")
    List<Conversation> findAllConversationsByUserId(String userId);

    @Query(value = "{ $or: [ { 'user1Id': ?0 }, { 'user2Id': ?0 } ] }", count = true)
    long countByUserId(String userId);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'lastMessage': ?1, 'lastMessageAt': ?2, 'updatedAt': ?2 }, '$inc': { 'version': 1 } }")
    void updateLastMessage(String conversationId, String lastMessage, LocalDateTime lastMessageAt);
}
