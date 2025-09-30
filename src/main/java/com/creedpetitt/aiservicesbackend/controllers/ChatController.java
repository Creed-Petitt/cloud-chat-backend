package com.creedpetitt.aiservicesbackend.controllers;

import com.creedpetitt.aiservicesbackend.aiservices.ChatService;
import com.creedpetitt.aiservicesbackend.aiservices.ChatServiceFactory;
import com.creedpetitt.aiservicesbackend.models.AppUser;
import com.creedpetitt.aiservicesbackend.models.Conversation;
import com.creedpetitt.aiservicesbackend.models.Message;
import com.creedpetitt.aiservicesbackend.services.ConversationService;
import com.creedpetitt.aiservicesbackend.services.MessageService;
import com.creedpetitt.aiservicesbackend.services.RateLimitingService;
import com.creedpetitt.aiservicesbackend.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import jakarta.servlet.http.HttpServletRequest;
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
    private final RateLimitingService rateLimitingService;

    public ChatController(ConversationService conversationService,
                         MessageService messageService,
                         UserService userService,
                         ChatServiceFactory chatServiceFactory,
                         RateLimitingService rateLimitingService) {
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.userService = userService;
        this.chatServiceFactory = chatServiceFactory;
        this.rateLimitingService = rateLimitingService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<Map<String, Object>>> getConversations(Authentication authentication) {
        if (authentication == null) {
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
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            AppUser user = getAuthenticatedUser(authentication);
            Optional<Conversation> conversationOpt = conversationService.getConversation(id, user);

            if (conversationOpt.isEmpty()) {
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
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        // Rate limiting check
        if (authentication != null) {
            // Authenticated user - check database limit
            AppUser user = getAuthenticatedUser(authentication);
            if (!rateLimitingService.isUserAllowed(user)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Rate limit exceeded");
                errorResponse.put("message", "You have reached the maximum of 20 messages. Please upgrade your account to continue.");
                errorResponse.put("remainingRequests", 0);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
            }
        } else {
            // Anonymous user - check IP-based limit
            String clientIP = getClientIP(httpRequest);
            if (!rateLimitingService.isAnonymousAllowed(clientIP)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Rate limit exceeded");
                errorResponse.put("message", "You have reached the maximum of 10 messages. Please sign in to continue using the service.");
                errorResponse.put("remainingRequests", 0);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
            }
        }

        try {
            String content = request.get("content");
            String aiModel = request.get("aiModel");
            
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            AppUser user = null;
            Conversation conversation = null;

            if (authentication != null) {
                user = getAuthenticatedUser(authentication);
                
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
            } else {

                if (aiModel == null || aiModel.trim().isEmpty()) {
                    return ResponseEntity.badRequest().build();
                }
            }

            ChatService chatService = chatServiceFactory.getChatService(aiModel != null ? aiModel : conversation.getAiModel());
            if (chatService == null) {
                return ResponseEntity.badRequest().build();
            }

            String aiResponse = chatService.getResponse(content);

            if (authentication != null) {

                userService.incrementMessageCount(user);

                Message userMessage = messageService.addUserMessage(conversation, user, content);
                Message aiMessage = messageService.addAssistantMessage(conversation, user, aiResponse);
                
                Map<String, Object> response = new HashMap<>();
                response.put("conversationId", conversation.getId());
                response.put("userMessage", messageToMap(userMessage));
                response.put("aiMessage", messageToMap(aiMessage));
                response.put("remainingRequests", rateLimitingService.getRemainingRequests(user));
                return ResponseEntity.ok(response);
            } else {

                String clientIP = getClientIP(httpRequest);
                rateLimitingService.incrementAnonymousCount(clientIP);

                Map<String, Object> response = new HashMap<>();
                response.put("conversationId", 0);
                response.put("userMessage", createTempMessage(content, "USER"));
                response.put("aiMessage", createTempMessage(aiResponse, "ASSISTANT"));
                response.put("remainingRequests", rateLimitingService.getRemainingAnonymousRequests(clientIP));
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/conversations/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        SseEmitter emitter = new SseEmitter(300000L); // 5 minute timeout

        // Rate limiting check
        if (authentication != null) {
            AppUser user = getAuthenticatedUser(authentication);
            if (!rateLimitingService.isUserAllowed(user)) {
                emitter.completeWithError(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded for user."));
                return emitter;
            }
            userService.incrementMessageCount(user);
        } else {
            String clientIP = getClientIP(httpRequest);
            if (!rateLimitingService.isAnonymousAllowed(clientIP)) {
                emitter.completeWithError(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded for anonymous user."));
                return emitter;
            }
            rateLimitingService.incrementAnonymousCount(clientIP);
        }

        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            emitter.completeWithError(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content cannot be empty."));
            return emitter;
        }

        String aiModel = request.get("aiModel");
        Conversation conversation = null;

        if (authentication != null && id != 0) {
            AppUser user = getAuthenticatedUser(authentication);
            Optional<Conversation> conversationOpt = conversationService.getConversation(id, user);
            if (conversationOpt.isEmpty()) {
                emitter.completeWithError(new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found."));
                return emitter;
            }
            conversation = conversationOpt.get();
        }

        if (aiModel == null || aiModel.trim().isEmpty()) {
            if (conversation != null) {
                aiModel = conversation.getAiModel();
            } else {
                emitter.completeWithError(new ResponseStatusException(HttpStatus.BAD_REQUEST, "aiModel must be provided for new conversations."));
                return emitter;
            }
        }

        ChatService chatService = chatServiceFactory.getChatService(aiModel);
        if (chatService == null) {
            emitter.completeWithError(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid aiModel specified."));
            return emitter;
        }

        // Create conversation and save user message if authenticated
        final Conversation finalConversation;
        final AppUser finalUser;

        if (authentication != null) {
            finalUser = getAuthenticatedUser(authentication);

            if (id == 0) {
                // Create new conversation
                String title = generateTitle(content);
                finalConversation = conversationService.createConversation(finalUser, title, aiModel);
            } else {
                finalConversation = conversation;
            }

            // Save user message
            messageService.addUserMessage(finalConversation, finalUser, content);
        } else {
            finalConversation = null;
            finalUser = null;
        }

        final Long conversationId = (finalConversation != null) ? finalConversation.getId() : null;

        // Subscribe to the Flux stream and send chunks via SseEmitter
        Flux<String> responseStream = chatService.getResponseStream(content);
        final StringBuilder fullResponse = new StringBuilder();

        responseStream.subscribe(
            chunk -> {
                try {
                    fullResponse.append(chunk);
                    emitter.send(SseEmitter.event().data(chunk));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            },
                emitter::completeWithError,
            () -> {
                // Save assistant message when streaming completes
                if (conversationId != null && finalUser != null) {
                    conversationService.getConversation(conversationId, finalUser)
                            .ifPresent(conv -> messageService.addAssistantMessage(conv, finalUser, fullResponse.toString()));
                }
                emitter.complete();
            }
        );

        return emitter;
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

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        return request.getRemoteAddr();
    }

    private Map<String, Object> createTempMessage(String content, String type) {
        Map<String, Object> message = new HashMap<>();
        message.put("id", 0);
        message.put("content", content);
        message.put("type", type);
        message.put("createdAt", java.time.LocalDateTime.now());
        return message;
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