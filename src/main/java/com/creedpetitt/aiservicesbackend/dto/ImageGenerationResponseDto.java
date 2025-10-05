package com.creedpetitt.aiservicesbackend.dto;

import java.time.LocalDateTime;

public record ImageGenerationResponseDto(
    String imageUrl,
    String prompt,
    String model,
    int remainingImages,
    LocalDateTime generatedAt,
    Long conversationId
) {
}
