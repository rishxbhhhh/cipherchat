package com.rishabh.cipherchat.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateConversationRequest {

    @NotNull
    private String type;

    @NotEmpty
    private List<String> participantEmails;
}
