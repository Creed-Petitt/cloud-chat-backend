package com.creedpetitt.aiservicesbackend.controllers;

import com.creedpetitt.aiservicesbackend.aiservices.ChatService;
import com.creedpetitt.aiservicesbackend.aiservices.ChatServiceFactory;
import com.creedpetitt.aiservicesbackend.dto.ConversationDetailDto;
import com.creedpetitt.aiservicesbackend.dto.ConversationDto;
import com.creedpetitt.aiservicesbackend.dto.MessageDto;
import com.creedpetitt.aiservicesbackend.dto.StreamMessageRequestDto;
import com.creedpetitt.aiservicesbackend.models.AppUser;
import com.creedpetitt.aiservicesbackend.models.Conversation;
import com.creedpetitt.aiservicesbackend.models.Message;
import com.creedpetitt.aiservicesbackend.repositories.UserRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// All exceptions are handled globally via GlobalExceptionHandler
@RestController
@RequestMapping("/api")
public class ChatController extends BaseController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ChatServiceFactory chatServiceFactory;
    private final RateLimitingService rateLimitingService;

    public ChatController(ConversationService conversationService,
                         MessageService messageService,
                         UserService userService,
                         UserRepository userRepository,
                         ChatServiceFactory chatServiceFactory,
                         RateLimitingService rateLimitingService) {
        super(userService, userRepository);
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.chatServiceFactory = chatServiceFactory;
        this.rateLimitingService = rateLimitingService;
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDto>> getConversations(Authentication authentication) {
        AppUser user = requireAuthenticatedUser(authentication);
        List<Conversation> conversations = conversationService.getUserConversations(user);

        List<ConversationDto> response = conversations.stream()
                .map(ConversationDto::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<ConversationDetailDto> getConversation(@PathVariable Long id, Authentication authentication) {
        AppUser user = requireAuthenticatedUser(authentication);
        Conversation conversation = conversationService.getConversation(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));

        List<Message> messages = messageService.getConversationMessages(conversation);

        List<MessageDto> messageList = messages.stream()
                .map(MessageDto::fromEntity)
                .collect(Collectors.toList());

        ConversationDetailDto response = new ConversationDetailDto(
                ConversationDto.fromEntity(conversation),
                messageList
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/conversations/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(
            @PathVariable Long id,
            @RequestBody StreamMessageRequestDto request,
            Authentication authentication) {

        SseEmitter emitter = new SseEmitter(300000L); // 5 minute timeout

        AppUser user = requireAuthenticatedUser(authentication);

        if (!rateLimitingService.isUserAllowed(user)) {
            emitter.completeWithError(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded for user."));
            return emitter;
        }
        userService.incrementMessageCount(user);

        String content = request.content();
        if (content == null || content.trim().isEmpty()) {
            emitter.completeWithError(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content cannot be empty."));
            return emitter;
        }

        String imageUrl = request.imageUrl();
        String aiModel = request.aiModel();
        Conversation conversation = null;

        if (id != 0) {
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

        // Create conversation and save user message
        final Conversation finalConversation;

        if (id == 0) {
            String title = generateTitle(content);
            finalConversation = conversationService.createConversation(user, title, aiModel);
        } else {
            finalConversation = conversation;
        }

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            messageService.addUserMessage(finalConversation, user, content, imageUrl);
        } else {
            messageService.addUserMessage(finalConversation, user, content);
        }

        final Long conversationId = finalConversation.getId();
        final AppUser finalUser = user;
        final String finalAiModel = aiModel;

        // Subscribe to the Flux stream and send chunks via SseEmitter
        Flux<String> responseStream;
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            responseStream = chatService.getResponseStream(content, imageUrl);
        } else {
            responseStream = chatService.getResponseStream(content);
        }
        final StringBuilder fullResponse = new StringBuilder();

        responseStream.subscribe(
            chunk -> {
                try {
                    fullResponse.append(chunk);
                    emitter.send(SseEmitter.event().data(chunk));
                } catch (Exception e) {
                    // Client disconnected or emitter already completed - ignore
                    try {
                        emitter.completeWithError(e);
                    } catch (IllegalStateException ignored) {
                        // Emitter already completed, ignore
                    }
                }
            },
            error -> {
                try {
                    emitter.completeWithError(error);
                } catch (IllegalStateException ignored) {
                    // Emitter already completed, ignore
                }
            },
            () -> {
                // Save assistant message when streaming completes
                if (!fullResponse.isEmpty()) {
                    conversationService.getConversation(conversationId, finalUser)
                            .ifPresent(conv -> messageService.addAssistantMessage(conv, finalUser, fullResponse.toString(), finalAiModel));
                }
                try {
                    emitter.complete();
                } catch (IllegalStateException ignored) {
                    // Emitter already completed, ignore
                }
            }
        );

        return emitter;
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Void> deleteConversation(@PathVariable Long id, Authentication authentication) {
        AppUser user = requireAuthenticatedUser(authentication);
        conversationService.deleteConversation(id, user);
        return ResponseEntity.noContent().build();
    }

    // Helper methods
    private String generateTitle(String content) {
        String title = content.trim();
        if (title.length() > 50) {
            title = title.substring(0, 47) + "...";
        }
        return title;
    }
}