package com.creedpetitt.aiservicesbackend.aiservices;

import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.stereotype.Service;

@Service("geminiService")
public class GeminiService implements ChatService {
    private final VertexAiGeminiChatModel chatModel;

    public GeminiService(VertexAiGeminiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String getResponse(String prompt) {
        return chatModel.call(prompt);
    }

    @Override
    public String getModel() {
        return "gemini";
    }
}