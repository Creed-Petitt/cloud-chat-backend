package com.creedpetitt.aiservicesbackend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class GenAiController {
    OpenAIService openAIService;
    GeminiService  geminiService;
    ClaudeService  claudeService;

    public GenAiController(OpenAIService openAIService, GeminiService geminiService,  ClaudeService claudeService) {
        this.openAIService = openAIService;
        this.geminiService = geminiService;
        this.claudeService = claudeService;
    }

    @GetMapping("openai-chat")
    public String getOpenAIResponse(@RequestParam String prompt) throws IOException {

        return openAIService.getResponse(prompt);
    }

    @GetMapping("gemini-chat")
    public String getGeminiResponse(@RequestParam String prompt) throws IOException {

        return geminiService.getResponse(prompt);
    }

    @GetMapping("claude-chat")
    public String getClaudeResponse(@RequestParam String prompt) throws IOException {

        return claudeService.getResponse(prompt);
    }

}
