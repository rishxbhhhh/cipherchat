package com.rishabh.cipherchat.service;

import com.rishabh.cipherchat.dto.SendMessageRequest;
import com.rishabh.cipherchat.dto.MessageResponse;
import org.springframework.data.domain.Page;

public interface MessageService {
    Long sendMessage(SendMessageRequest request, String senderEmail);
    Page<MessageResponse> getHistory(Long conversationId, int page, int size, String userEmail);
}
