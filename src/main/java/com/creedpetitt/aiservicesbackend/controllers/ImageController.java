package com.creedpetitt.aiservicesbackend.controllers;

import com.creedpetitt.aiservicesbackend.aiservices.ImagenService;
import com.creedpetitt.aiservicesbackend.aiservices.OpenAIService;
import com.creedpetitt.aiservicesbackend.models.AppUser;
import com.creedpetitt.aiservicesbackend.models.Conversation;
import com.creedpetitt.aiservicesbackend.models.Message;
import com.creedpetitt.aiservicesbackend.services.ConversationService;
import com.creedpetitt.aiservicesbackend.services.MessageService;
import com.creedpetitt.aiservicesbackend.services.RateLimitingService;
import com.creedpetitt.aiservicesbackend.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final OpenAIService openAIService;
    private final ImagenService imagenService;
    private final RateLimitingService rateLimitingService;
    private final UserService userService;
    private final MessageService messageService;
    private final ConversationService conversationService;

    public ImageController(OpenAIService openAIService,
                          ImagenService imagenService,
                          RateLimitingService rateLimitingService,
                          UserService userService,
                          MessageService messageService,
                          ConversationService conversationService) {
        this.openAIService = openAIService;
        this.imagenService = imagenService;
        this.rateLimitingService = rateLimitingService;
        this.userService = userService;
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
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Image rate limit exceeded");
                errorResponse.put("message", "You have reached the maximum of 5 images. Please upgrade your account to continue.");
                errorResponse.put("remainingImages", 0);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
            }
        } else {
            // Anonymous user - check IP-based limit
            String clientIP = getClientIP(httpRequest);
            if (!rateLimitingService.isAnonymousImageAllowed(clientIP)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Image rate limit exceeded");
                errorResponse.put("message", "You have reached the maximum of 3 images. Please sign in to continue using the service.");
                errorResponse.put("remainingImages", 0);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
            }
        }

        try {

            String prompt = request.get("prompt");
            if (prompt == null || prompt.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Prompt is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String model = request.getOrDefault("model", "imagen");
            String conversationIdStr = request.get("conversationId");

            String imageUrl;
            if ("dalle".equalsIgnoreCase(model)) {
                imageUrl = openAIService.generateImage(prompt);
            } else {
                imageUrl = imagenService.generateImage(prompt);
            }

            // Save conversation and messages only for authenticated users
            if (authentication != null) {
                Conversation conversation;
                if (conversationIdStr != null && !conversationIdStr.isEmpty()) {
                    long conversationId = Long.parseLong(conversationIdStr);
                    conversation = conversationService.getConversationById(conversationId)
                            .orElseThrow(() -> new RuntimeException("Conversation not found"));
                } else {
                    conversation = new Conversation(user, "Image Generation", model);
                    conversation = conversationService.saveConversation(conversation);
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
                String clientIP = getClientIP(httpRequest);
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
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error generating image");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/generate-test")
    public ResponseEntity<String> generateImageTest(
            @RequestParam String prompt,
            @RequestParam(defaultValue = "imagen") String model) {
        try {
            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Prompt is required");
            }

            String imageUrl;
            if ("dalle".equalsIgnoreCase(model)) {
                imageUrl = openAIService.generateImage(prompt);
            } else {
                imageUrl = imagenService.generateImage(prompt);
            }

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", imageUrl)
                    .body("Redirecting to image: " + imageUrl);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating image: " + e.getMessage());
        }
    }

    @GetMapping("/my-images")
    public ResponseEntity<List<Map<String, Object>>> getMyImages(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            AppUser user = getAuthenticatedUser(authentication);
            List<Message> images = messageService.getUserImageMessages(user);
            List<Map<String, Object>> response = images.stream()
                    .map(this::messageToMap)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Map<String, Object> messageToMap(Message message) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", message.getId());
        map.put("content", message.getContent());
        map.put("messageType", message.getMessageType().toString()); // Renamed from "type" to be more specific
        map.put("imageUrl", message.getImageUrl());
        map.put("createdAt", message.getCreatedAt());
        return map;
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
}