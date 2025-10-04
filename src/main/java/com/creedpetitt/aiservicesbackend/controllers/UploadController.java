package com.creedpetitt.aiservicesbackend.controllers;

import com.creedpetitt.aiservicesbackend.services.ImageUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UploadController {

    private final ImageUploadService imageUploadService;

    public UploadController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @PostMapping("/upload/image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        try {
            String fileUrl = imageUploadService.uploadUserImage(file);

            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", fileUrl);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to upload image: " + e.getMessage(), e);
        }
    }
}
