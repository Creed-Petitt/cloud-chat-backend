package com.creedpetitt.aiservicesbackend.dto;

public record MessageRequest(
        String content,
        String aiModel
) {
}