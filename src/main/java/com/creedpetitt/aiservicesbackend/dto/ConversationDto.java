package com.creedpetitt.aiservicesbackend.dto;

import com.creedpetitt.aiservicesbackend.models.Conversation;

import java.time.LocalDateTime;

public record ConversationDto(
    Long id,
    String title,
    String aiModel,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static ConversationDto fromEntity(Conversation conversation) {
        return new ConversationDto(
            conversation.getId(),
            conversation.getTitle(),
            conversation.getAiModel(),
            conversation.getCreatedAt(),
            conversation.getUpdatedAt()
        );
    }
}
