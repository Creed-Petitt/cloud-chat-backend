package com.creedpetitt.aiservicesbackend.aiservices;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service("claudeService")
public class ClaudeService extends ChatService {
    private final AnthropicChatModel chatModel;

    public ClaudeService(AnthropicChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    protected ChatModel getChatModel() {
        return chatModel;
    }

    @Override
    public String getModel() {
        return "claude";
    }
}
