package com.creedpetitt.aiservicesbackend.dto;

import com.creedpetitt.aiservicesbackend.models.Message;
import com.creedpetitt.aiservicesbackend.models.Message.MessageType;

import java.time.LocalDateTime;

public record MessageDto(
    Long id,
    String content,
    MessageType messageType,
    String imageUrl,
    String aiModel,
    LocalDateTime createdAt
) {
    public static MessageDto fromEntity(Message message) {
        return new MessageDto(
            message.getId(),
            message.getContent(),
            message.getMessageType(),
            message.getImageUrl(),
            message.getAiModel(),
            message.getCreatedAt()
        );
    }
}
