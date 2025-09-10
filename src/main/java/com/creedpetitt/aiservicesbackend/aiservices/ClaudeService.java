package com.creedpetitt.aiservicesbackend.aiservices;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.stereotype.Service;

@Service("claudeService")
public class ClaudeService implements ChatService {
    private final AnthropicChatModel chatModel;

    public ClaudeService(AnthropicChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public String getResponse(String prompt) {
        return chatModel.call(prompt);
    }

    @Override
    public String getModel() {
        return "claude";
    }
}