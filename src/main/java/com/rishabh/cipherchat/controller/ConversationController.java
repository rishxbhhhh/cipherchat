package com.rishabh.cipherchat.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rishabh.cipherchat.dto.CreateConversationRequest;
import com.rishabh.cipherchat.service.ConversationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createConversation(@RequestBody @Valid CreateConversationRequest request,
            Authentication authentication) {
        Long id = conversationService.createConversation(request, authentication.getName());
        return ResponseEntity.ok(Map.of("conversationId", id));
    }
}
