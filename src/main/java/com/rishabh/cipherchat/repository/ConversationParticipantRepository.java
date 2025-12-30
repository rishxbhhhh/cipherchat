package com.rishabh.cipherchat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rishabh.cipherchat.entity.ConversationParticipant;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, Long> {
    
}
