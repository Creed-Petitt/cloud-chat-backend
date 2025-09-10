package com.creedpetitt.aiservicesbackend.aiservices;

public interface ChatService {
    String getResponse(String prompt);
    String getModel();
}
