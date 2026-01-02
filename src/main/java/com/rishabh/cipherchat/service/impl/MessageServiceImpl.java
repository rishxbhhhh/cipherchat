package com.rishabh.cipherchat.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rishabh.cipherchat.dto.SendMessageRequest;
import com.rishabh.cipherchat.entity.Conversation;
import com.rishabh.cipherchat.entity.Message;
import com.rishabh.cipherchat.entity.User;
import com.rishabh.cipherchat.exception.ForbiddenException;
import com.rishabh.cipherchat.exception.ResourceNotFoundException;
import com.rishabh.cipherchat.repository.ConversationParticipantRepository;
import com.rishabh.cipherchat.repository.ConversationRepository;
import com.rishabh.cipherchat.repository.MessageRepository;
import com.rishabh.cipherchat.repository.UserRepository;
import com.rishabh.cipherchat.service.MessageService;

@Service
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;

    public MessageServiceImpl(MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            UserRepository userRepository,
            ConversationParticipantRepository conversationParticipantRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.conversationParticipantRepository = conversationParticipantRepository;
    }

    @Override
    @Transactional
    @SuppressWarnings("null")
    public Long sendMessage(SendMessageRequest request, String senderEmail) {
        Conversation conversation = conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));
        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found."));
        if (conversationParticipantRepository.existsByConversationIdAndUserEmail(request.getConversationId(),
                senderEmail) == false) {
            throw new ForbiddenException("User is not a part of this conversation.");
        }
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(request.getContent());
        messageRepository.save(message);
        return message.getId();
    }
}
