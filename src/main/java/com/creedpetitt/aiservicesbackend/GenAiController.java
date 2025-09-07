package com.creedpetitt.aiservicesbackend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GenAiController {

    private final ChatServiceFactory chatServiceFactory;

    public GenAiController(ChatServiceFactory chatServiceFactory) {
        this.chatServiceFactory = chatServiceFactory;
    }

    @GetMapping("/chat")
    public String getResponse(@RequestParam String model, @RequestParam String prompt) {
        ChatService chatService = chatServiceFactory.getChatService(model);
        if (chatService == null) {
            return "Invalid model specified.";
        }
        return chatService.getResponse(prompt);
    }
}