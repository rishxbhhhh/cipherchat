package com.rishabh.cipherchat.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rishabh.cipherchat.entity.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
}
