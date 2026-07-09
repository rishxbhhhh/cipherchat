package com.rishabh.cipherchat.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.rishabh.cipherchat.dto.SendMessageRequest;
import com.rishabh.cipherchat.service.MessageService;

import jakarta.validation.Valid;

@Controller
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    public ChatWebSocketController(SimpMessagingTemplate messagingTemplate, MessageService messageService) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
    }

    @MessageMapping("/chat")
    public void handleChat(@Payload @Valid SendMessageRequest request, Principal principal) {
        Long messageId = messageService.sendMessage(request, principal.getName());

        messagingTemplate.convertAndSend(
                "/topic/conversation/" + request.getConversationId(),
                new WebSocketMessage(messageId, principal.getName(), request.getContent()));
    }

    public record WebSocketMessage(Long messageId, String senderEmail, String content) {}
}
