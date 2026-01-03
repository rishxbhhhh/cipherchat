package com.rishabh.cipherchat.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageResponse {
    
    private Long id;
    private String senderEmail;
    private String content;
    private LocalDateTime sentAt;
}
