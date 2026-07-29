package com.example.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public class UiChatDtos {

    @Schema(description = "Represents a chat session in the sidebar")
    public record SessionInfoDto(
            String chatId,
            String title
    ) {}

    @Schema(description = "Represents a single message in the chat UI")
    public record ChatMessageDto(
            String role,
            String content
    ) {}

    @Schema(description = "Represents the full history of a chat session")
    public record ChatTranscriptDto(
            String chatId,
            List<ChatMessageDto> messages
    ) {}
}