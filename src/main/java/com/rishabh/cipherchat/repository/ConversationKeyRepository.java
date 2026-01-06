package com.rishabh.cipherchat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rishabh.cipherchat.entity.ConversationKey;

public interface ConversationKeyRepository extends JpaRepository<ConversationKey, Long> {
        Optional<ConversationKey> findByConversationIdAndUserId(Long conversationId, Long userId);
}
