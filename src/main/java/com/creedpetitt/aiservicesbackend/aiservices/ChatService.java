package com.creedpetitt.aiservicesbackend.aiservices;

import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.UrlResource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;

public abstract class ChatService {

    protected static final String SYSTEM_PROMPT =
        "Format responses in clean markdown:\n" +
        "- Use ## for section headers\n" +
        "- Separate sections/paragraphs with double line breaks\n" +
        "- Indent nested bullets with 4 spaces\n" +
        "- Use code fences (```) with language tags for code\n\n" +
        "Example:\n" +
        "## Header\n\n" +
        "Text here.\n\n" +
        "* Point\n" +
        "    * Sub-point";

    protected abstract ChatModel getChatModel();

    public abstract String getModel();

    public boolean supportsVision() {
        return true;
    }

    public Flux<String> getResponseStream(String prompt) {
        SystemMessage systemMessage = new SystemMessage(SYSTEM_PROMPT);
        UserMessage userMessage = new UserMessage(prompt);
        return getChatModel().stream(new Prompt(List.of(systemMessage, userMessage)))
                .mapNotNull(chatResponse ->
                        Optional.ofNullable(chatResponse)
                                .map(ChatResponse::getResult)
                                .map(Generation::getOutput)
                                .map(AbstractMessage::getText)
                                .orElse(null));
    }

    public Flux<String> getResponseStream(String prompt, String imageUrl) {
        try {
            MimeType mimeType = detectMimeType(imageUrl);
            var userMessage = UserMessage.builder()
                    .text(prompt)
                    .media(List.of(new Media(mimeType, new UrlResource(imageUrl))))
                    .build();
            SystemMessage systemMessage = new SystemMessage(SYSTEM_PROMPT);

            return getChatModel().stream(new Prompt(List.of(systemMessage, userMessage)))
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

    protected MimeType detectMimeType(String url) {
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
}
