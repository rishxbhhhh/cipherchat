package com.rishabh.cipherchat.service;

import java.util.List;

import com.rishabh.cipherchat.dto.ConversationListResponse;
import com.rishabh.cipherchat.dto.CreateConversationRequest;

public interface ConversationService {
    Long createConversation(CreateConversationRequest request, String creatorEmail);
    List<ConversationListResponse> listConversations(String userEmail);
    void renameConversation(Long conversationId, String userEmail, String newName);
}
