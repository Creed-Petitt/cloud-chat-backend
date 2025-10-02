package com.creedpetitt.aiservicesbackend.aiservices;

import reactor.core.publisher.Flux;

public interface ChatService {
    Flux<String> getResponseStream(String prompt);
    Flux<String> getResponseStream(String prompt, String imageUrl);
    String getModel();
    boolean supportsVision();
}
