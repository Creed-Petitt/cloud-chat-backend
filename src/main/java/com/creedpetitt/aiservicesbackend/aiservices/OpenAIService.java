package com.creedpetitt.aiservicesbackend.aiservices;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.stereotype.Service;

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
        return chatModel.call(prompt);
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