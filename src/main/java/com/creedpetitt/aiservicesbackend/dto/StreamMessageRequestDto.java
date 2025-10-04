package com.creedpetitt.aiservicesbackend.dto;

public record StreamMessageRequestDto(
    String content,
    String imageUrl,
    String aiModel
) {}
