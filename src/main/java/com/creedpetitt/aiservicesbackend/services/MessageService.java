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

    public Message addUserMessage(Conversation conversation, AppUser user, String content, String imageUrl) {
        if (conversation == null || user == null || content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Conversation, user and content cannot be null or empty");
        }

        Message message = new Message(conversation, user, content.trim(), Message.MessageType.USER, imageUrl);
        Message savedMessage = messageRepository.save(message);

        conversationService.updateConversationTimestamp(conversation);
        
        return savedMessage;
    }

    public Message addAssistantMessage(Conversation conversation, AppUser user, String content, String aiModel) {
        if (conversation == null || user == null || content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Conversation, user and content cannot be null or empty");
        }

        Message message = new Message(conversation, user, content.trim(), Message.MessageType.ASSISTANT);
        message.setAiModel(aiModel);
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
        return messageRepository.findAllImageMessagesByUser(user);
    }

    @Transactional
    public void recordImageGeneration(Conversation conversation, AppUser user, String prompt, String imageUrl, String aiModel) {
        Message userMessage = new Message(conversation, user, prompt, Message.MessageType.USER);
        messageRepository.save(userMessage);

        Message imageMessage = new Message(conversation, user, "Generated Image", Message.MessageType.ASSISTANT, imageUrl);
        imageMessage.setAiModel(aiModel);
        messageRepository.save(imageMessage);

        conversationService.updateConversationTimestamp(conversation);
    }


}