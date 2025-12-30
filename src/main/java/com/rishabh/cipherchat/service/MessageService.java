package com.rishabh.cipherchat.service;

import com.rishabh.cipherchat.dto.SendMessageRequest;

public interface MessageService {
    Long sendMessage(SendMessageRequest request, String senderEmail);
}
