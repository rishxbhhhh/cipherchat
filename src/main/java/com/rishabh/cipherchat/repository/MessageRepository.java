package com.rishabh.cipherchat.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.rishabh.cipherchat.entity.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByConversationIdOrderBySentAtDesc(Long conversationId, Pageable pageable);
    
    Optional<Message> findTopByConversationIdOrderBySentAtDesc(Long conversationId);
}
