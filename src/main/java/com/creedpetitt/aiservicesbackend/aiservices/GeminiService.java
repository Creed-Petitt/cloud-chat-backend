package com.creedpetitt.aiservicesbackend.aiservices;

import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Optional;

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
    public Flux<String> getResponseStream(String prompt) {
        return chatModel.stream(new Prompt(prompt))
                .mapNotNull(chatResponse ->
                        Optional.ofNullable(chatResponse)
                                .map(ChatResponse::getResult)
                                .map(Generation::getOutput)
                                .map(AbstractMessage::getText)
                                .orElse(null));
    }

    @Override
    public String getModel() {
        return "gemini";
    }
}