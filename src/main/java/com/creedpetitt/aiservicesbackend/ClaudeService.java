package com.creedpetitt.aiservicesbackend;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.stereotype.Service;

@Service
public class ClaudeService {
    private final AnthropicChatModel chatModel;
    public ClaudeService(AnthropicChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String getResponse(String prompt) {
        return chatModel.call(prompt);
    }

}
