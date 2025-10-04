package com.creedpetitt.aiservicesbackend.aiservices;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.stereotype.Service;

@Service("openAIService")
public class OpenAIService extends ChatService {
    private final OpenAiChatModel chatModel;
    private final OpenAiImageModel imageModel;

    public OpenAIService(OpenAiChatModel chatModel, OpenAiImageModel imageModel) {
        this.chatModel = chatModel;
        this.imageModel = imageModel;
    }

    @Override
    protected ChatModel getChatModel() {
        return chatModel;
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
