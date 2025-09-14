package com.creedpetitt.aiservicesbackend.controllers;

import com.creedpetitt.aiservicesbackend.aiservices.ChatService;
import com.creedpetitt.aiservicesbackend.aiservices.ChatServiceFactory;
import com.creedpetitt.aiservicesbackend.models.AppUser;
import com.creedpetitt.aiservicesbackend.models.Conversation;
import com.creedpetitt.aiservicesbackend.models.Message;
import com.creedpetitt.aiservicesbackend.services.ConversationService;
import com.creedpetitt.aiservicesbackend.services.MessageService;
import com.creedpetitt.aiservicesbackend.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final UserService userService;
    private final ChatServiceFactory chatServiceFactory;

    public ChatController(ConversationService conversationService,
                         MessageService messageService,
                         UserService userService,
                         ChatServiceFactory chatServiceFactory) {
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.userService = userService;
        this.chatServiceFactory = chatServiceFactory;
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<Map<String, Object>>> getConversations(Authentication authentication) {
        System.out.println("ChatController.getConversations called");
        System.out.println("Authentication: " + (authentication != null ? authentication.getName() : "NULL"));
        
        if (authentication == null) {
            System.out.println("Authentication is null, returning 401");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            AppUser user = getAuthenticatedUser(authentication);
            List<Conversation> conversations = conversationService.getUserConversations(user);
            
            List<Map<String, Object>> response = conversations.stream()
                    .map(this::conversationToMap)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<Map<String, Object>> getConversation(@PathVariable Long id, Authentication authentication) {
        System.out.println("-> Entering getConversation for id: " + id);
        if (authentication == null) {
            System.out.println("<- Exiting getConversation: UNAUTHORIZED");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            AppUser user = getAuthenticatedUser(authentication);
            Optional<Conversation> conversationOpt = conversationService.getConversation(id, user);

            if (conversationOpt.isEmpty()) {
                System.out.println("<- Exiting getConversation: NOT FOUND");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            Conversation conversation = conversationOpt.get();
            List<Message> messages = messageService.getConversationMessages(conversation);

            List<Map<String, Object>> messageList = messages.stream()
                    .map(this::messageToMap)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("conversation", conversationToMap(conversation));
            response.put("messages", messageList);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String content = request.get("content");
            String aiModel = request.get("aiModel");
            
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            AppUser user = getAuthenticatedUser(authentication);
            Conversation conversation;

            if (id == 0) {
                if (aiModel == null || aiModel.trim().isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
                conversation = conversationService.createConversation(user, generateTitle(content), aiModel);
            } else {
                Optional<Conversation> conversationOpt = conversationService.getConversation(id, user);
                if (conversationOpt.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
                conversation = conversationOpt.get();
            }

            ChatService chatService = chatServiceFactory.getChatService(conversation.getAiModel());
            if (chatService == null) {
                return ResponseEntity.badRequest().build();
            }

            Message userMessage = messageService.addUserMessage(conversation, user, content);

            List<Message> context = messageService.getConversationMessages(conversation);
            String contextPrompt = buildContextPrompt(context.subList(0, Math.min(context.size(), 20))); // Last 20 messages
            String aiResponse = chatService.getResponse(contextPrompt);

            Message aiMessage = messageService.addAssistantMessage(conversation, user, aiResponse);

            Map<String, Object> response = new HashMap<>();
            response.put("conversationId", conversation.getId());
            response.put("userMessage", messageToMap(userMessage));
            response.put("aiMessage", messageToMap(aiMessage));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AppUser user = getAuthenticatedUser(authentication);
        conversationService.deleteConversation(id, user);
        return ResponseEntity.noContent().build();
    }

    // Helper methods
    private AppUser getAuthenticatedUser(Authentication authentication) {
        String uid = authentication.getName();
        String email = (String) authentication.getDetails();
        return userService.getOrCreateUser(uid, email != null ? email : uid + "@firebase.user");
    }

    private Map<String, Object> conversationToMap(Conversation conversation) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", conversation.getId());
        map.put("title", conversation.getTitle());
        map.put("aiModel", conversation.getAiModel());
        map.put("createdAt", conversation.getCreatedAt());
        map.put("updatedAt", conversation.getUpdatedAt());
        return map;
    }

    private Map<String, Object> messageToMap(Message message) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", message.getId());
        map.put("content", message.getContent());
        map.put("type", message.getMessageType().toString());
        map.put("createdAt", message.getCreatedAt());
        return map;
    }

    private String buildContextPrompt(List<Message> messages) {
        if (messages.isEmpty()) {
            return "";
        }

        StringBuilder prompt = new StringBuilder();
        for (Message msg : messages) {
            String role = msg.isUserMessage() ? "User" : "Assistant";
            prompt.append(role).append(": ").append(msg.getContent()).append("\n");
        }
        return prompt.toString();
    }

    private String generateTitle(String content) {
        String title = content.trim();
        if (title.length() > 50) {
            title = title.substring(0, 47) + "...";
        }
        return title;
    }
}