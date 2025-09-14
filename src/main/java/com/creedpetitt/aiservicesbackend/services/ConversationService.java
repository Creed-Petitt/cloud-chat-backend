package com.creedpetitt.aiservicesbackend.services;

import com.creedpetitt.aiservicesbackend.models.AppUser;
import com.creedpetitt.aiservicesbackend.models.Conversation;
import com.creedpetitt.aiservicesbackend.repositories.ConversationRepository;
import com.creedpetitt.aiservicesbackend.repositories.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public ConversationService(ConversationRepository conversationRepository, MessageRepository messageRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    public Conversation createConversation(AppUser user, String title, String aiModel) {
        if (user == null || aiModel == null || aiModel.trim().isEmpty()) {
            throw new IllegalArgumentException("User and AI model cannot be null or empty");
        }
        
        // Auto-generate title if null or empty
        if (title == null || title.trim().isEmpty()) {
            title = "New Chat";
        }

        Conversation conversation = new Conversation(user, title, aiModel);
        return conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public Optional<Conversation> getConversation(Long conversationId, AppUser user) {
        if (conversationId == null || user == null) {
            return Optional.empty();
        }
        return conversationRepository.findByIdAndUser(conversationId, user);
    }

    @Transactional(readOnly = true)
    public List<Conversation> getUserConversations(AppUser user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        return conversationRepository.findByUserOrderByUpdatedAtDesc(user);
    }

    public void updateConversationTimestamp(Conversation conversation) {
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
    }

    public void deleteConversation(Long conversationId, AppUser user) {
        conversationRepository.findByIdAndUser(conversationId, user).ifPresent(conversation -> {
            messageRepository.deleteAllByConversation(conversation);
            conversationRepository.delete(conversation);
        });
    }
}