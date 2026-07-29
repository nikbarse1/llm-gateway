package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
@Slf4j
public class LLMSummarizationService {

    // Conservative chunk limit (e.g., GPT-4o-mini / Llama 3)
    private static final int CHUNK_TOKEN_LIMIT = 3000;
    private static final int CHUNK_CONCURRENCY = 1;

    private final ChatClient chatClient;
    private final TokenCounterService tokenCounterService;

    public LLMSummarizationService(
            @Qualifier("openAiChatModel") ChatModel fastTierChatModel,
            TokenCounterService tokenCounterService) {
        this.chatClient = ChatClient.create(fastTierChatModel);
        this.tokenCounterService = tokenCounterService;
        log.info("LLMSummarizationService configured using Spring AI Fast-Tier Engine.");
    }

    public Mono<String> smartCompress(String text, OptimizationRequest.TargetType targetType) {
        if (text == null || text.isBlank()) {
            return Mono.just("Empty input.");
        }

        // Split text strictly by token counts, entirely removing character-limit guesswork
        List<String> chunks = tokenCounterService.splitTextByTokens(text, CHUNK_TOKEN_LIMIT);

        if (chunks.size() == 1) {
            return callFastTier(buildSmartUserPrompt(chunks.get(0), targetType), chunks.get(0));
        }

        log.info("Input is large ({} tokens) - splitting into {} chunks. Processing sequentially.",
                tokenCounterService.countTokens(text), chunks.size());

        return Flux.fromIterable(chunks)
                .index()
                .flatMapSequential(indexed -> callFastTier(
                        buildMapUserPrompt(indexed.getT2(), indexed.getT1().intValue() + 1, chunks.size()),
                        indexed.getT2()), CHUNK_CONCURRENCY)
                .collectList()
                .flatMap(extractedNotes -> {
                    String combinedNotes = String.join("\n\n", extractedNotes);

                    // If the combined notes are STILL too large, we do a recursive compression pass.
                    // This prevents the final map-reduce step from crashing the context window.
                    if (tokenCounterService.countTokens(combinedNotes) > CHUNK_TOKEN_LIMIT) {
                        return smartCompress(combinedNotes, targetType);
                    }

                    return callFastTier(buildSmartUserPrompt(combinedNotes, targetType), combinedNotes);
                });
    }

    private String getBaseRules() {
        return """
               CRITICAL RULES:
               1. DO NOT summarize, alter, or truncate any source code, JSON, XML, scripts, or configuration files. Preserve all syntax and code blocks verbatim.
               2. Remove filler words, conversational pleasantries, and redundant prose.
               3. Optimize for token efficiency while retaining 100% of the technical accuracy and factual data.
               """;
    }

    private String buildSmartUserPrompt(String text, OptimizationRequest.TargetType targetType) {
        if (targetType == OptimizationRequest.TargetType.INSTRUCTION) {
            return "Refine and condense the following instruction. Preserve all requirements and constraints. Be concise.\n\nInput:\n" + text;
        } else if (targetType == OptimizationRequest.TargetType.HISTORY) {
            return """
                   Extract and append immutable facts, key technical decisions, variables, and architectural constraints from this conversation transcript. 
                   Do NOT write prose or paragraphs. Output STRICTLY as a highly condensed, bulleted key-value ledger.
                   
                   Transcript to Process:
                   """ + text;
        } else {
            return "Compress the following document. Retain all factual data and parameters. Output strictly as bulleted notes.\n\nDocument:\n" + text;
        }
    }

    private String buildMapUserPrompt(String chunk, int partIndex, int totalParts) {
        return "You are helping compress a large input (Part " + partIndex + " of " + totalParts + ").\n\n"
                + "Extract every important fact, decision, instruction, and ALL code verbatim as concise notes. Output ONLY the extracted notes:\n\n" + chunk;
    }

    private Mono<String> callFastTier(String userPrompt, String fallbackSource) {
        return Mono.fromCallable(() -> chatClient.prompt()
                        .system(getBaseRules())
                        .user(userPrompt)
                        .call()
                        .content())
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("Error calling Fast-Tier API: {}, using fallback", e.getMessage());
                    return Mono.just(fallbackSource);
                });
    }
}