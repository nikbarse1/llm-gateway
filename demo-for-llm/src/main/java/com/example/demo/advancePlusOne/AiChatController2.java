package com.example.demo.advancePlusOne;

import com.example.demo.llmrouter.AiChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "AI Chat Gateway", description = "Stateful, token-optimized conversational AI routing engine")
public class AiChatController2 {

    private final AdvancedGatewayOrchestrationService gatewayOrchestrationService;
    private final ActiveSessionTracker sessionTracker;
    private final ChatTitleGeneratorService titleGeneratorService;

    @Operation(summary = "Submit a prompt to the optimized AI gateway")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<AiChatResponse> chat(
            @RequestParam("instruction") String instruction,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "chatId", required = false) String chatId,
            @RequestParam(value = "provider", defaultValue = "GEMINI") String provider,
            @RequestParam(value = "contextWindow", defaultValue = "8192") int contextWindow,
            @RequestParam(value = "promptId", required = false) String promptId,
            @RequestHeader(value = "X-Developer-Mode", defaultValue = "false") boolean isDevMode
    ) {
        boolean isNewSession = (chatId == null || chatId.isBlank());
        String activeChatId = isNewSession ? UUID.randomUUID().toString() : chatId;

        // If this is a brand new chat, register it and generate a title in the background
        if (isNewSession && !sessionTracker.sessionExists(activeChatId)) {
            sessionTracker.registerSession(activeChatId, "New Chat");

            // Fire-and-forget background task for title generation
            titleGeneratorService.generateTitle(instruction)
                    .doOnNext(title -> sessionTracker.updateTitle(activeChatId, title))
                    .subscribe();
        }

        log.info("Incoming gateway request - ChatId: {}, Provider: {}, File: {}, URL: {}",
                activeChatId, provider, (file != null && !file.isEmpty()), (url != null && !url.isBlank()));
        log.info("Request payload - chatId: {}, instructionLength: {}, instructionPreview: '{}'",
                activeChatId, instruction.length(), truncate(instruction, 200));

        return gatewayOrchestrationService.processStatefulChat(
                instruction, file, url, activeChatId, provider, contextWindow, isDevMode, promptId
        ).map(response -> {
            response.setChatId(activeChatId);
            log.info("Response payload - chatId: {}, sourceType: {}, wasOptimized: {}, responseLength: {}",
                    activeChatId, response.getSourceType(), response.isWasOptimized(),
                    response.getUserReadableMessage() != null ? response.getUserReadableMessage().length() : 0);
            return response;
        });
    }

    private static String truncate(String text, int max) {
        if (text == null || text.length() <= max) return text;
        return text.substring(0, max) + "...";
    }
}