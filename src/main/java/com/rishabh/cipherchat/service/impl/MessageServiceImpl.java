package com.rishabh.cipherchat.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.rishabh.cipherchat.dto.SendMessageRequest;
import com.rishabh.cipherchat.dto.MessageResponse;
import com.rishabh.cipherchat.entity.Conversation;
import com.rishabh.cipherchat.entity.Message;
import com.rishabh.cipherchat.entity.User;
import com.rishabh.cipherchat.entity.ConversationKey;
import com.rishabh.cipherchat.exception.ForbiddenException;
import com.rishabh.cipherchat.exception.ResourceNotFoundException;
import com.rishabh.cipherchat.repository.ConversationParticipantRepository;
import com.rishabh.cipherchat.repository.ConversationRepository;
import com.rishabh.cipherchat.repository.MessageRepository;
import com.rishabh.cipherchat.repository.UserRepository;
import com.rishabh.cipherchat.repository.ConversationKeyRepository;
import com.rishabh.cipherchat.service.EncryptionService;
import com.rishabh.cipherchat.service.MessageService;
import com.rishabh.cipherchat.service.KeyService;

@Service
public class MessageServiceImpl implements MessageService {
        private final MessageRepository messageRepository;
        private final ConversationRepository conversationRepository;
        private final UserRepository userRepository;
        private final ConversationParticipantRepository conversationParticipantRepository;
        private final EncryptionService encryptionService;
        private final KeyService keyService;
        private final ConversationKeyRepository conversationKeyRepository;

        public MessageServiceImpl(MessageRepository messageRepository,
                        ConversationRepository conversationRepository,
                        UserRepository userRepository,
                        ConversationParticipantRepository conversationParticipantRepository,
                        EncryptionService encryptionService, ConversationKeyRepository conversationKeyRepository,
                        KeyService keyService) {

                this.messageRepository = messageRepository;
                this.conversationRepository = conversationRepository;
                this.userRepository = userRepository;
                this.conversationParticipantRepository = conversationParticipantRepository;
                this.encryptionService = encryptionService;
                this.conversationKeyRepository = conversationKeyRepository;
                this.keyService = keyService;
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

                ConversationKey ck = conversationKeyRepository
                                .findByConversationIdAndUserId(conversation.getId(), sender.getId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Conversation key not found for user."));

                // decrypt user's private key first
                byte[] privateKeyBytes = keyService.decryptPrivateKey(sender.getPrivateKeyEncrypted());

                // rebuild PrivateKey object
                PrivateKey privateKey;
                try {
                        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                        privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
                } catch (Exception e) {
                        throw new RuntimeException("Failed to rebuild private key.", e);
                }

                // decrypt conversation key
                byte[] conversationKeyBytes = encryptionService.decryptWithPrivateKey(ck.getConversationKey(),
                                privateKey);

                // now encrypt message body with conversationKeyBytes (AES/GCM)
                String encryptedMessage = encryptionService.encryptWithAesKey(request.getContent(),
                                conversationKeyBytes);

                Message message = new Message();
                message.setConversation(conversation);
                message.setSender(sender);
                message.setContent(encryptedMessage);
                messageRepository.save(message);
                return message.getId();
        }

        @Override
        @Transactional(readOnly = true)
        public Page<MessageResponse> getHistory(Long conversationId, int page, int size, String userEmail) {
                if (conversationParticipantRepository.existsByConversationIdAndUserEmail(conversationId,
                                userEmail) == false) {
                        throw new ForbiddenException("User is not a part of this conversation.");
                }
                User sender = userRepository.findByEmail(userEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
                ConversationKey ck = conversationKeyRepository
                                .findByConversationIdAndUserId(conversationId, sender.getId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Conversation key not found for user."));

                // decrypt user's private key first
                byte[] privateKeyBytes = keyService.decryptPrivateKey(sender.getPrivateKeyEncrypted());

                // rebuild PrivateKey object
                PrivateKey privateKey;
                try {
                        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                        privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
                } catch (Exception e) {
                        throw new RuntimeException("Failed to rebuild private key.", e);
                }

                // decrypt conversation key
                byte[] conversationKeyBytes = encryptionService.decryptWithPrivateKey(ck.getConversationKey(),
                                privateKey);
                Page<Message> messages = messageRepository.findByConversationIdOrderBySentAtDesc(conversationId,
                                PageRequest.of(page, size));
                return messages.map(message -> new MessageResponse(
                                message.getId(),
                                message.getSender().getEmail(),
                                encryptionService.decryptWithAesKey(message.getContent(), conversationKeyBytes),
                                message.getSentAt()));
        }
}