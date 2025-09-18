package com.creedpetitt.aiservicesbackend.controllers;

import com.creedpetitt.aiservicesbackend.aiservices.OpenAIService;
import com.creedpetitt.aiservicesbackend.models.AppUser;
import com.creedpetitt.aiservicesbackend.services.RateLimitingService;
import com.creedpetitt.aiservicesbackend.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final OpenAIService openAIService;
    private final RateLimitingService rateLimitingService;
    private final UserService userService;

    public ImageController(OpenAIService openAIService,
                          RateLimitingService rateLimitingService,
                          UserService userService) {
        this.openAIService = openAIService;
        this.rateLimitingService = rateLimitingService;
        this.userService = userService;
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateImage(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            AppUser user = getAuthenticatedUser(authentication);

            if (!rateLimitingService.isUserImageAllowed(user)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Image rate limit exceeded");
                errorResponse.put("message", "You have reached the maximum of 3 images. Please upgrade your account to continue.");
                errorResponse.put("remainingImages", 0);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(errorResponse);
            }

            String prompt = request.get("prompt");
            if (prompt == null || prompt.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Prompt is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String imageUrl = openAIService.generateImage(prompt);

            userService.incrementImageCount(user);

            Map<String, Object> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            response.put("prompt", prompt);
            response.put("remainingImages", rateLimitingService.getRemainingImages(user));
            response.put("generatedAt", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error generating image");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/generate-test")
    public ResponseEntity<String> generateImageTest(@RequestParam String prompt) {
        try {
            if (prompt == null || prompt.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Prompt is required");
            }
            String imageUrl = openAIService.generateImage(prompt);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", imageUrl)
                    .body("Redirecting to image: " + imageUrl);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating image: " + e.getMessage());
        }
    }

    // Helper method
    private AppUser getAuthenticatedUser(Authentication authentication) {
        String uid = authentication.getName();
        String email = (String) authentication.getDetails();
        return userService.getOrCreateUser(uid, email != null ? email : uid + "@firebase.user");
    }
}