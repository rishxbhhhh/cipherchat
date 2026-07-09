package com.rishabh.cipherchat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.rishabh.cipherchat.entity.ConversationParticipant;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {
    boolean existsByConversationIdAndUserEmail(Long conversationId, String email);

    @Query("SELECT cp FROM ConversationParticipant cp JOIN FETCH cp.conversation WHERE cp.user.email = :email")
    List<ConversationParticipant> findByUserEmailWithConversation(String email);
}
