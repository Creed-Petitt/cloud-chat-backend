package com.creedpetitt.aiservicesbackend.aiservices;

import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;

@Service("openAIService")
public class OpenAIService implements ChatService {
    private final OpenAiChatModel chatModel;
    private final OpenAiImageModel imageModel;

    public OpenAIService(OpenAiChatModel chatModel, OpenAiImageModel imageModel) {
        this.chatModel = chatModel;
        this.imageModel = imageModel;
    }

    @Override
    public Flux<String> getResponseStream(String prompt) {
        String systemPrompt = "Always format your responses in proper markdown. Use code fences (```) with language tags for code blocks.";
        return chatModel.stream(new Prompt(systemPrompt + "\n\n" + prompt))
                .mapNotNull(chatResponse ->
                        Optional.ofNullable(chatResponse)
                                .map(ChatResponse::getResult)
                                .map(Generation::getOutput)
                                .map(AbstractMessage::getText)
                                .orElse(null));
    }

    @Override
    public String getModel() {
        return "openai";
    }

    @Override
    public boolean supportsVision() {
        return true;
    }

    @Override
    public Flux<String> getResponseStream(String prompt, String imageUrl) {
        try {
            MimeType mimeType = detectMimeType(imageUrl);
            var userMessage = UserMessage.builder()
                    .text(prompt)
                    .media(List.of(new Media(mimeType, new UrlResource(imageUrl))))
                    .build();

            return chatModel.stream(new Prompt(userMessage))
                    .mapNotNull(chatResponse ->
                            Optional.ofNullable(chatResponse)
                                    .map(ChatResponse::getResult)
                                    .map(Generation::getOutput)
                                    .map(AbstractMessage::getText)
                                    .orElse(null));
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid image URL: " + imageUrl, e);
        }
    }

    private MimeType detectMimeType(String url) {
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.endsWith(".pdf")) {
            return new MimeType("application", "pdf");
        } else if (lowerUrl.endsWith(".png")) {
            return MimeTypeUtils.IMAGE_PNG;
        } else if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) {
            return MimeTypeUtils.IMAGE_JPEG;
        } else if (lowerUrl.endsWith(".gif")) {
            return MimeTypeUtils.IMAGE_GIF;
        } else if (lowerUrl.endsWith(".webp")) {
            return new MimeType("image", "webp");
        }
        // Default to PNG if unknown
        return MimeTypeUtils.IMAGE_PNG;
    }

    public String generateImage(String prompt) {
        ImageResponse response = imageModel.call(new ImagePrompt(prompt));
        return response.getResult().getOutput().getUrl();
    }
}
