package com.example.demo.advancePlusOne;

import com.example.demo.dto.UiChatDtos.ChatMessageDto;
import com.example.demo.dto.UiChatDtos.ChatTranscriptDto;
import com.example.demo.dto.UiChatDtos.SessionInfoDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v2/chat-history")
@RequiredArgsConstructor
@Tag(name = "Chat History UI API", description = "Endpoints for rendering the frontend chat interface")
@Slf4j
public class ChatHistoryController {

    private final ChatMemory chatMemory;
    private final ActiveSessionTracker sessionTracker;

    @Operation(summary = "Get all active chat sessions for the sidebar with auto-generated titles")
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionInfoDto>> getAllSessions() {
        List<SessionInfoDto> sessions = sessionTracker.getAllSessions().entrySet().stream()
                .map(entry -> new SessionInfoDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        log.info("GET /api/v2/chat-history/sessions returned {} sessions", sessions.size());
        return ResponseEntity.ok(sessions);
    }

    @Operation(summary = "Load the full transcript for a specific chat window")
    @GetMapping("/{chatId}")
    public ResponseEntity<ChatTranscriptDto> getChatTranscript(
            @Parameter(description = "The unique ID of the chat session")
            @PathVariable String chatId) {

        List<Message> rawMessages = chatMemory.get(chatId);

        if (rawMessages == null || rawMessages.isEmpty()) {
            log.warn("Chat transcript not found for chatId={}", chatId);
            return ResponseEntity.notFound().build();
        }

        List<ChatMessageDto> uiMessages = rawMessages.stream()
                .filter(msg -> !msg.getMessageType().getValue().equalsIgnoreCase("SYSTEM"))
                .map(msg -> new ChatMessageDto(
                        msg.getMessageType().getValue().toUpperCase(),
                        msg.getText()
                ))
                .collect(Collectors.toList());

        log.info("Loaded transcript for chatId={} with {} messages", chatId, uiMessages.size());
        return ResponseEntity.ok(new ChatTranscriptDto(chatId, uiMessages));
    }

    @Operation(summary = "Delete a chat session")
    @DeleteMapping("/{chatId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String chatId) {
        chatMemory.clear(chatId);
        sessionTracker.clearSession(chatId);
        log.info("Deleted chat session chatId={}", chatId);
        return ResponseEntity.noContent().build();
    }
}