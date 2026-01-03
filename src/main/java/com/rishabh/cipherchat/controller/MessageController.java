package com.rishabh.cipherchat.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rishabh.cipherchat.dto.SendMessageRequest;
import com.rishabh.cipherchat.service.MessageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody @Valid SendMessageRequest request,
            Authentication authentication) {
        Long messageId = messageService.sendMessage(request, authentication.getName());
        return ResponseEntity.ok(Map.of("messageId", messageId));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getMessageHistory(@RequestParam Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Page<?> result = messageService.getHistory(conversationId, page, size, authentication.getName());
        return ResponseEntity.ok(result);
    }
}
