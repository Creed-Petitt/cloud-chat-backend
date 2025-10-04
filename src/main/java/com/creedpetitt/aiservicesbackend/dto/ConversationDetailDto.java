package com.creedpetitt.aiservicesbackend.dto;

import java.util.List;

public record ConversationDetailDto(
    ConversationDto conversation,
    List<MessageDto> messages
) {}
