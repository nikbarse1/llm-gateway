package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class TokenOptimizationService {

    private final TokenCounterService tokenCounterService;
    private final LLMSummarizationService llmSummarizationService;

    public TokenOptimizationService(TokenCounterService tokenCounterService, LLMSummarizationService llmSummarizationService) {
        this.tokenCounterService = tokenCounterService;
        this.llmSummarizationService = llmSummarizationService;
    }

    public Mono<OptimizationResponse> optimizeDocument(OptimizationRequest request) {
        String rawText = request.getDocument();
        int originalTokens = tokenCounterService.countTokens(rawText);

        log.info("Starting smart optimization for {} (Original Tokens: {})", request.getTargetType(), originalTokens);

        // Delegate completely to the LLM for smart compression
        return llmSummarizationService.smartCompress(rawText, request.getTargetType())
                .map(summary -> {
                    int summaryTokens = tokenCounterService.countTokens(summary);
                    log.info("Completed smart optimization for {}. New Tokens: {}", request.getTargetType(), summaryTokens);
                    return buildResponse(originalTokens, summaryTokens, summary);
                });
    }

    private OptimizationResponse buildResponse(int originalTokens, int summaryTokens, String content) {
        // Map directly to the new temporary tracking fields.
        // The AdvancedGatewayOrchestrationService will extract these, do the final math,
        // and populate the clean nested JSON structure.
        return OptimizationResponse.builder()
                .tempFastTierInputTokens(originalTokens)
                .tempFastTierOutputTokens(summaryTokens)
                .tempSummary(content)
                .build();
    }
}