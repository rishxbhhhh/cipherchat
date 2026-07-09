package com.rishabh.cipherchat.dto;

import java.time.LocalDateTime;

public record ConversationListResponse(
    Long id,
    String type,
    String name,
    LocalDateTime createdAt
) {}
