package com.rishabh.cipherchat.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.rishabh.cipherchat.dto.CreateConversationRequest;
import com.rishabh.cipherchat.entity.Conversation;
import com.rishabh.cipherchat.entity.ConversationParticipant;
import com.rishabh.cipherchat.entity.ConversationType;
import com.rishabh.cipherchat.entity.User;
import com.rishabh.cipherchat.repository.ConversationParticipantRepository;
import com.rishabh.cipherchat.repository.ConversationRepository;
import com.rishabh.cipherchat.repository.UserRepository;
import com.rishabh.cipherchat.service.ConversationService;

import jakarta.transaction.Transactional;

@Service
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;

    public ConversationServiceImpl(ConversationRepository conversationRepository, UserRepository userRepository,
            ConversationParticipantRepository conversationParticipantRepository) {
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.conversationParticipantRepository = conversationParticipantRepository;
    }

    @Override
    @Transactional
    public Long createConversation(CreateConversationRequest request, String creatorEmail) {
        ConversationType type = ConversationType.valueOf(request.getType().toUpperCase());

        HashSet<String> emails = new HashSet<>(request.getParticipantEmails());
        emails.add(creatorEmail);

        List<User> participants = userRepository.findAllByEmailIn(new ArrayList<>(emails))
                .orElseThrow(() -> new IllegalStateException("no users found."));

        if (participants.size() != emails.size()) {
            throw new IllegalStateException("One or more users not found.");
        }

        Conversation conversation = new Conversation();
        conversation.setType(type);
        conversationRepository.save(conversation);

        for (User u : participants) {
            ConversationParticipant cp = new ConversationParticipant();
            cp.setConversation(conversation);
            cp.setUser(u);
            conversationParticipantRepository.save(cp);
        }

        return conversation.getId();
    }
}
