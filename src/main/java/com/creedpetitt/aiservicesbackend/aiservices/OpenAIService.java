package com.creedpetitt.aiservicesbackend.aiservices;

import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Optional;

@Service("openAIService")
public class OpenAIService implements ChatService {
    private final OpenAiChatModel chatModel;
    private final OpenAiImageModel imageModel;

    public OpenAIService(OpenAiChatModel chatModel, OpenAiImageModel imageModel) {
        this.chatModel = chatModel;
        this.imageModel = imageModel;
    }

    @Override
    public String getResponse(String prompt) {
        String systemPrompt = "Always format your responses in proper markdown. Use code fences (```) with language tags for code blocks.";
        return chatModel.call(systemPrompt + "\n\n" + prompt);
    }

    @Override
    public Flux<String> getResponseStream(String prompt) {
        String systemPrompt = "Always format your responses in proper markdown. Use code fences (```) with language tags for code blocks.";
        return chatModel.stream(new Prompt(systemPrompt + "\n\n" + prompt))
                .mapNotNull(chatResponse ->
                        Optional.ofNullable(chatResponse)
                                .map(ChatResponse::getResult)
                                .map(Generation::getOutput)
                                .map(AbstractMessage::getText)
                                .orElse(null));
    }

    @Override
    public String getModel() {
        return "openai";
    }

    public String generateImage(String prompt) {
        ImageResponse response = imageModel.call(new ImagePrompt(prompt));
        return response.getResult().getOutput().getUrl();
    }
}
