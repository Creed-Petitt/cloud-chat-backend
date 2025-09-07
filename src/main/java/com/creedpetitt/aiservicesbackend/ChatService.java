package com.creedpetitt.aiservicesbackend;

public interface ChatService {
    String getResponse(String prompt);
    String getModel();
}
