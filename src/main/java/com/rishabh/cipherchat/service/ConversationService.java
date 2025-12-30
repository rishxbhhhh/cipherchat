package com.rishabh.cipherchat.service;

import com.rishabh.cipherchat.dto.CreateConversationRequest;

public interface ConversationService {
    Long createConversation(CreateConversationRequest request, String creatorEmail);
}
