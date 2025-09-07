package com.creedpetitt.aiservicesbackend;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChatServiceFactory {
    private final Map<String, ChatService> chatServiceMap;

    public ChatServiceFactory(List<ChatService> chatServices) {
        this.chatServiceMap = chatServices.stream()
                .collect(Collectors.toMap(ChatService::getModel, Function.identity()));
    }

    public ChatService getChatService(String model) {
        return chatServiceMap.get(model);
    }
}
