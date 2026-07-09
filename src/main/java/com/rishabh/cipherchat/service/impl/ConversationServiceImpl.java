package com.rishabh.cipherchat.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.rishabh.cipherchat.dto.ConversationListResponse;
import com.rishabh.cipherchat.dto.CreateConversationRequest;
import com.rishabh.cipherchat.entity.Conversation;
import com.rishabh.cipherchat.entity.ConversationKey;
import com.rishabh.cipherchat.entity.ConversationParticipant;
import com.rishabh.cipherchat.entity.ConversationType;
import com.rishabh.cipherchat.entity.Role;
import com.rishabh.cipherchat.entity.User;
import com.rishabh.cipherchat.exception.BadRequestException;
import com.rishabh.cipherchat.exception.ForbiddenException;
import com.rishabh.cipherchat.exception.ResourceNotFoundException;
import com.rishabh.cipherchat.repository.ConversationKeyRepository;
import com.rishabh.cipherchat.repository.ConversationParticipantRepository;
import com.rishabh.cipherchat.repository.ConversationRepository;
import com.rishabh.cipherchat.repository.UserRepository;
import com.rishabh.cipherchat.service.ConversationService;
import com.rishabh.cipherchat.service.EncryptionService;

import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final EncryptionService encryptionService;
    private final ConversationKeyRepository conversationKeyRepository;

    public ConversationServiceImpl(ConversationRepository conversationRepository, UserRepository userRepository,
            ConversationParticipantRepository conversationParticipantRepository, EncryptionService encryptionService,
            ConversationKeyRepository conversationKeyRepository) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.conversationParticipantRepository = conversationParticipantRepository;
        this.encryptionService = encryptionService;
        this.conversationKeyRepository = conversationKeyRepository;
    }

    @Override
    @Transactional
    public Long createConversation(CreateConversationRequest request, String creatorEmail) {
        ConversationType type = ConversationType.valueOf(request.getType().toUpperCase());

        HashSet<String> emails = new HashSet<>(request.getParticipantEmails());
        emails.add(creatorEmail);

        // Admin accounts cannot participate in conversations
        List<User> participants = userRepository.findAllByEmailIn(new ArrayList<>(emails))
                .orElseThrow(() -> new ResourceNotFoundException("no users found."));

        if (participants.size() != emails.size()) {
            throw new ResourceNotFoundException("One or more users not found.");
        }

        // Admin accounts cannot chat
        boolean hasAdmin = participants.stream().anyMatch(u -> u.getRole() == Role.ADMIN);
        if (hasAdmin) {
            throw new ForbiddenException("Admin accounts cannot participate in conversations.");
        }

        if (type == ConversationType.PRIVATE) {
            if (emails.size() != 2) {
                throw new BadRequestException("Private conversations must have exactly two participants.");
            }
            Conversation existingConversation = conversationRepository.findPrivateConversationByParticipants(emails)
                    .orElse(null);
            if (existingConversation != null) {
                return existingConversation.getId();
            }
        }

        byte[] conversationKey = encryptionService.generateAesKey();

        Conversation conversation = new Conversation();
        conversation.setType(type);
        conversationRepository.save(conversation);

        for (User u : participants) {
            ConversationParticipant cp = new ConversationParticipant();
            cp.setConversation(conversation);
            cp.setUser(u);
            conversationParticipantRepository.save(cp);
        
            String encryptedConversationKey = encryptionService.encryptForUser(conversationKey, u.getPublicKey());
            ConversationKey ck = new ConversationKey();
            ck.setConversation(conversation);
            ck.setUser(u);
            ck.setConversationKey(encryptedConversationKey);
            conversationKeyRepository.save(ck);
        }

        return conversation.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationListResponse> listConversations(String userEmail) {
        return conversationParticipantRepository.findByUserEmailWithConversation(userEmail)
                .stream()
                .map(cp -> {
                    Conversation c = cp.getConversation();
                    return new ConversationListResponse(
                            c.getId(),
                            c.getType().name(),
                            "Conversation " + c.getId(),
                            c.getCreatedAt());
                })
                .collect(Collectors.toList());
    }
}
