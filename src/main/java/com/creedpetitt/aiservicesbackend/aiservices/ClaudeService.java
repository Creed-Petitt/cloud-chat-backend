package com.creedpetitt.aiservicesbackend.aiservices;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Optional;

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
    public Flux<String> getResponseStream(String prompt) {
        return chatModel.stream(new Prompt(prompt))
                .mapNotNull(chatResponse -> chatResponse.getResult().getOutput().getText());
    }

    @Override
    public String getModel() {
        return "claude";
    }
}