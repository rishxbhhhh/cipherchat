package com.rishabh.cipherchat.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.rishabh.cipherchat.entity.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("""
            SELECT c FROM Conversation c
            JOIN ConversationParticipant p ON p.conversation.id = c.id
            WHERE c.type = 'PRIVATE'
            AND p.user.email IN (:emails)
            GROUP BY c.id
            HAVING COUNT(p.id) = 2
            """)
    Optional<Conversation> findPrivateConversationByParticipants(java.util.Set<String> emails);
}
