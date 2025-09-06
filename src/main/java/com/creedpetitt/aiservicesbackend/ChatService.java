package com.creedpetitt.aiservicesbackend;

import org.springframework.ai.chat.model.ChatModel;

public class ChatService {
    private final ChatModel chatModel;

    public ChatService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String getResponse(String prompt) {
        return chatModel.call(prompt);
    }
}
