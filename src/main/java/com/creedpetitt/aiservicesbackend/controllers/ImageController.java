package com.creedpetitt.aiservicesbackend.controllers;

import com.creedpetitt.aiservicesbackend.aiservices.ImageService;
import com.creedpetitt.aiservicesbackend.aiservices.ImageServiceFactory;
import com.creedpetitt.aiservicesbackend.dto.MessageDto;
import com.creedpetitt.aiservicesbackend.models.AppUser;
import com.creedpetitt.aiservicesbackend.models.Conversation;
import com.creedpetitt.aiservicesbackend.models.Message;
import com.creedpetitt.aiservicesbackend.services.ConversationService;
import com.creedpetitt.aiservicesbackend.services.MessageService;
import com.creedpetitt.aiservicesbackend.services.RateLimitingService;
import com.creedpetitt.aiservicesbackend.services.UserService;
import com.creedpetitt.aiservicesbackend.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageController extends BaseController {

    private final ImageServiceFactory imageServiceFactory;
    private final RateLimitingService rateLimitingService;
    private final MessageService messageService;
    private final ConversationService conversationService;

    public ImageController(ImageServiceFactory imageServiceFactory,
                          RateLimitingService rateLimitingService,
                          UserService userService,
                          MessageService messageService,
                          ConversationService conversationService) {
        super(userService);
        this.imageServiceFactory = imageServiceFactory;
        this.rateLimitingService = rateLimitingService;
        this.messageService = messageService;
        this.conversationService = conversationService;
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateImage(
            @RequestBody Map<String, String> request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        // Rate limiting check
        AppUser user = null;
        if (authentication != null) {
            // Authenticated user - check database limit
            user = getAuthenticatedUser(authentication);
            if (!rateLimitingService.isUserImageAllowed(user)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "You have reached the maximum of 5 images. Please upgrade your account to continue.");
            }
        } else {
            // Anonymous user - check IP-based limit
            String clientIP = RequestUtils.getClientIP(httpRequest);
            if (!rateLimitingService.isAnonymousImageAllowed(clientIP)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "You have reached the maximum of 3 images. Please sign in to continue using the service.");
            }
        }

        String prompt = request.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prompt is required");
        }

        String model = request.getOrDefault("model", "imagen");
        String conversationIdStr = request.get("conversationId");

        ImageService imageService = imageServiceFactory.getImageService(model);
        if (imageService == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid model specified");
        }

        try {
            String imageUrl = imageService.generateImage(prompt);

            // Save conversation and messages only for authenticated users
            if (authentication != null) {
                Conversation conversation;
                if (conversationIdStr != null && !conversationIdStr.isEmpty()) {
                    long conversationId = Long.parseLong(conversationIdStr);
                    conversation = conversationService.getConversationById(conversationId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found"));
                } else {
                    conversation = conversationService.createConversation(user, "Image Generation", model);
                }

                messageService.recordImageGeneration(conversation, user, prompt, imageUrl);

                userService.incrementImageCount(user);

                Map<String, Object> response = new HashMap<>();
                response.put("imageUrl", imageUrl);
                response.put("prompt", prompt);
                response.put("model", model);
                response.put("remainingImages", rateLimitingService.getRemainingImages(user));
                response.put("generatedAt", java.time.LocalDateTime.now());
                response.put("conversationId", conversation.getId());

                return ResponseEntity.ok(response);
            } else {
                // Anonymous user - just return the image without saving
                String clientIP = RequestUtils.getClientIP(httpRequest);
                rateLimitingService.incrementAnonymousImageCount(clientIP);

                Map<String, Object> response = new HashMap<>();
                response.put("imageUrl", imageUrl);
                response.put("prompt", prompt);
                response.put("model", model);
                response.put("remainingImages", rateLimitingService.getRemainingAnonymousImages(clientIP));
                response.put("generatedAt", java.time.LocalDateTime.now());
                response.put("conversationId", 0);

                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error generating image: " + e.getMessage(), e);
        }
    }

    @GetMapping("/my-images")
    public ResponseEntity<List<MessageDto>> getMyImages(Authentication authentication) {
        AppUser user = requireAuthenticatedUser(authentication);
        List<Message> images = messageService.getUserImageMessages(user);
        List<MessageDto> response = images.stream()
                .map(MessageDto::fromEntity)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(response);
    }
}