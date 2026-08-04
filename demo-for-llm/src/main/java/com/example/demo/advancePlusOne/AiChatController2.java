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
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<AiChatResponse> chat(
            @RequestParam("instruction") String instruction,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "chatId", required = false) String chatId,
            @RequestParam(value = "provider", defaultValue = "") String provider,
            @RequestParam(value = "contextWindow", defaultValue = "8192") int contextWindow,
            @RequestParam(value = "promptId", required = false) String promptId,
            @RequestHeader(value = "X-Developer-Mode", defaultValue = "false") boolean isDevMode
    ) {

        if (provider == null || provider.isBlank() || "AUTO".equalsIgnoreCase(provider)) {
            provider = "";
        }

        boolean isNewSession = (chatId == null || chatId.isBlank());
        String activeChatId = isNewSession ? UUID.randomUUID().toString() : chatId;

        if (isNewSession && !sessionTracker.sessionExists(activeChatId)) {
            sessionTracker.registerSession(activeChatId, "New Chat");

            titleGeneratorService.generateTitle(instruction)
                    .doOnNext(title -> sessionTracker.updateTitle(activeChatId, title))
                    .subscribe();
        }

        // ===================== REQUEST LOG =====================
        log.info("========== Incoming AI Request ==========");

        log.info("chatId           : {}", activeChatId);
        log.info("isNewSession     : {}", isNewSession);
        log.info("provider         : {}", provider);
        log.info("contextWindow    : {}", contextWindow);
        log.info("developerMode    : {}", isDevMode);
        log.info("promptId         : {}", promptId);

        log.info("instructionLength: {}",
                instruction != null ? instruction.length() : 0);

        log.info("instruction      : {}",
                truncate(instruction, 500));

        log.info("url              : {}",
                url);

        if (file != null && !file.isEmpty()) {
            log.info("filePresent      : true");
            log.info("fileName         : {}", file.getOriginalFilename());
            log.info("contentType      : {}", file.getContentType());
            log.info("fileSize(bytes)  : {}", file.getSize());
        } else {
            log.info("filePresent      : false");
        }

        log.info("=========================================");

        return gatewayOrchestrationService.processStatefulChat(
                instruction,
                file,
                url,
                activeChatId,
                provider,
                contextWindow,
                isDevMode,
                promptId
        ).map(response -> {

            response.setChatId(activeChatId);

            // ===================== RESPONSE LOG =====================
            log.info("========== AI Response ==========");

            log.info("chatId           : {}", activeChatId);
            log.info("sourceType       : {}", response.getSourceType());
            log.info("wasOptimized     : {}", response.isWasOptimized());

            log.info("responseLength   : {}",
                    response.getUserReadableMessage() != null
                            ? response.getUserReadableMessage().length()
                            : 0);

            log.info("responsePreview  : {}",
                    truncate(response.getUserReadableMessage(), 500));

            log.info("=================================");

            return response;
        });
    }

    private static String truncate(String text, int max) {
        if (text == null || text.length() <= max) return text;
        return text.substring(0, max) + "...";
    }
}