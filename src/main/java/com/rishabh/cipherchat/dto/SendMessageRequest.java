package com.rishabh.cipherchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {
    
    @NotNull
    private Long conversationId;

    @NotBlank
    private String content;
}
