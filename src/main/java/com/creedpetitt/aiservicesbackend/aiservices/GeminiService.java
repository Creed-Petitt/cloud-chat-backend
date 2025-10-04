package com.creedpetitt.aiservicesbackend.aiservices;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.stereotype.Service;

@Service("geminiService")
public class GeminiService extends ChatService {
    private final VertexAiGeminiChatModel chatModel;

    public GeminiService(VertexAiGeminiChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    protected ChatModel getChatModel() {
        return chatModel;
    }

    @Override
    public String getModel() {
        return "gemini";
    }
}
