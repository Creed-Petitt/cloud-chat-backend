package com.creedpetitt.aiservicesbackend.services;

import com.creedpetitt.aiservicesbackend.models.AppUser;
import com.creedpetitt.aiservicesbackend.models.Conversation;
import com.creedpetitt.aiservicesbackend.models.Message;
import com.creedpetitt.aiservicesbackend.repositories.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;

    public MessageService(MessageRepository messageRepository, ConversationService conversationService) {
        this.messageRepository = messageRepository;
        this.conversationService = conversationService;
    }

    public Message saveMessage(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null");
        }
        return messageRepository.save(message);
    }

    public Message addUserMessage(Conversation conversation, AppUser user, String content) {
        if (conversation == null || user == null || content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Conversation, user and content cannot be null or empty");
        }

        Message message = new Message(conversation, user, content.trim(), Message.MessageType.USER);
        Message savedMessage = messageRepository.save(message);

        conversationService.updateConversationTimestamp(conversation);
        
        return savedMessage;
    }

    public Message addAssistantMessage(Conversation conversation, AppUser user, String content) {
        if (conversation == null || user == null || content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Conversation, user and content cannot be null or empty");
        }

        Message message = new Message(conversation, user, content.trim(), Message.MessageType.ASSISTANT);
        Message savedMessage = messageRepository.save(message);

        conversationService.updateConversationTimestamp(conversation);
        
        return savedMessage;
    }

    @Transactional(readOnly = true)
    public List<Message> getConversationMessages(Conversation conversation) {
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation cannot be null");
        }
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
    }

    @Transactional(readOnly = true)
    public List<Message> getUserImageMessages(AppUser user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        return messageRepository.findAllByUserAndMessageType(user, Message.MessageType.IMAGE);
    }


}