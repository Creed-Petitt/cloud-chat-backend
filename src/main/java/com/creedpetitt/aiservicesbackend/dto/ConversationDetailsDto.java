package com.creedpetitt.aiservicesbackend.dto;

import com.creedpetitt.aiservicesbackend.models.Message;

import java.util.List;

public record ConversationDetailsDto(
        ConversationDto conversationDto,
        List<MessageDto> messagesDto
) {
    public static ConversationDetailsDto fromEntity(ConversationDto conversationDto,
                             List<MessageDto> messagesDto) {
        return new ConversationDetailsDto(
                conversationDto,
                messagesDto
        );
    };
}
