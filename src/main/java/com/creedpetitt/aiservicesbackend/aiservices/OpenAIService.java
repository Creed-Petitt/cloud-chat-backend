package com.creedpetitt.aiservicesbackend.aiservices;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;

@Service("openAIService")
public class OpenAIService implements ChatService {
    private final OpenAiChatModel chatModel;

    public OpenAIService(OpenAiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String getResponse(String prompt) {
        return chatModel.call(prompt);
    }

    @Override
    public String getModel() {
        return "openai";
    }
}