package com.creedpetitt.aiservicesbackend.aiservices;

import reactor.core.publisher.Flux;

public interface ChatService {
    String getResponse(String prompt);
    Flux<String> getResponseStream(String prompt);
    String getModel();
}
