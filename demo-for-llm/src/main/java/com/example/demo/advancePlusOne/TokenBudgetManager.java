package com.example.demo.advancePlusOne;

import com.example.demo.OptimizationRequest.TargetType;
import com.example.demo.TokenCounterService;
import com.example.demo.TokenOptimizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenBudgetManager {

    private final TokenBudgetConfig config;
    private final TokenCounterService tokenCounterService;
    private final TokenOptimizationService tokenOptimizationService;

    public TokenBudget createBudget() {
        int total = config.getTotal();
        int system = config.getSystemReserve();
        int history = config.getHistoryReserve();
        int rag = config.getRagReserve();
        int user = config.getUserReserve();
        int response = config.getResponseReserve();

        int reserved = system + history + rag + user + response;
        if (reserved > total) {
            log.warn("Token budget reserves ({}) exceed total budget ({}). Scaling proportionally.", reserved, total);
            double scale = (double) total / reserved;
            system = (int) (system * scale);
            history = (int) (history * scale);
            rag = (int) (rag * scale);
            user = (int) (user * scale);
            response = (int) (response * scale);
        }

        TokenBudget budget = TokenBudget.builder()
                .totalBudget(total)
                .systemReserve(system)
                .historyReserve(history)
                .ragReserve(rag)
                .userReserve(user)
                .responseReserve(response)
                .actions(new ArrayList<>())
                .build();

        log.info("TokenBudget allocated — Total: {} | System: {} | History: {} | RAG: {} | User: {} | Response: {}",
                total, system, history, rag, user, response);
        return budget;
    }

    public Mono<EnforcementResult> enforceHistoryBudget(String historyText, TokenBudget budget) {
        int tokens = tokenCounterService.countTokens(historyText);
        int limit = budget.getHistoryReserve();

        if (tokens <= limit || historyText == null || historyText.isBlank()) {
            budget.addAction(new TokenBudget.BudgetAction("HISTORY", tokens, tokens, limit, "PASS_THROUGH"));
            log.info("History budget check — tokens: {}/{} ✓ (within reserve)", tokens, limit);
            return Mono.just(new EnforcementResult(historyText, tokens, tokens, "PASS_THROUGH"));
        }

        log.info("History budget exceeded — tokens: {}/{} → compressing", tokens, limit);
        return compressToBudget(historyText, tokens, limit, TargetType.HISTORY, budget, "HISTORY");
    }

    public Mono<EnforcementResult> enforceRagBudget(String ragContext, TokenBudget budget) {
        int tokens = tokenCounterService.countTokens(ragContext);
        int limit = budget.getRagReserve();

        if (tokens <= limit || ragContext == null || ragContext.isBlank()) {
            budget.addAction(new TokenBudget.BudgetAction("RAG", tokens, tokens, limit, "PASS_THROUGH"));
            log.info("RAG budget check — tokens: {}/{} ✓ (within reserve)", tokens, limit);
            return Mono.just(new EnforcementResult(ragContext, tokens, tokens, "PASS_THROUGH"));
        }

        log.info("RAG budget exceeded — tokens: {}/{} → truncating chunks", tokens, limit);
        String truncated = truncateToTokenBudget(ragContext, limit);
        int truncatedTokens = tokenCounterService.countTokens(truncated);
        budget.addAction(new TokenBudget.BudgetAction("RAG", tokens, truncatedTokens, limit, "CHUNK_TRUNCATION"));
        log.info("RAG truncated — tokens: {}/{} (dropped {} tokens)", truncatedTokens, limit, tokens - truncatedTokens);
        return Mono.just(new EnforcementResult(truncated, tokens, truncatedTokens, "CHUNK_TRUNCATION"));
    }

    public Mono<EnforcementResult> enforcePromptBudget(String promptText, TokenBudget budget) {
        int tokens = tokenCounterService.countTokens(promptText);
        int limit = budget.getUserReserve();

        if (tokens <= limit || promptText == null || promptText.isBlank()) {
            budget.addAction(new TokenBudget.BudgetAction("USER_PROMPT", tokens, tokens, limit, "PASS_THROUGH"));
            log.info("Prompt budget check — tokens: {}/{} ✓ (within reserve)", tokens, limit);
            return Mono.just(new EnforcementResult(promptText, tokens, tokens, "PASS_THROUGH"));
        }

        log.info("Prompt budget exceeded — tokens: {}/{} → compressing", tokens, limit);
        return compressToBudget(promptText, tokens, limit, TargetType.INSTRUCTION, budget, "USER_PROMPT");
    }

    public int calculateOptimalRagTopK(int estimatedChunkTokens, TokenBudget budget) {
        if (estimatedChunkTokens <= 0) {
            return config.getDefaultRagTopK();
        }
        int optimal = budget.getRagReserve() / estimatedChunkTokens;
        return Math.max(config.getMinRagTopK(), Math.min(optimal, config.getDefaultRagTopK()));
    }

    private Mono<EnforcementResult> compressToBudget(
            String text, int originalTokens, int limit,
            TargetType targetType, TokenBudget budget, String section) {

        return tokenOptimizationService.optimizeDocument(buildOptimizationRequest(text, limit, targetType))
                .map(response -> {
                    String compressed = response.getTempSummary();
                    int compressedTokens = tokenCounterService.countTokens(compressed);

                    if (compressedTokens > limit) {
                        compressed = truncateToTokenBudget(compressed, limit);
                        compressedTokens = tokenCounterService.countTokens(compressed);
                    }

                    budget.addAction(new TokenBudget.BudgetAction(section, originalTokens, compressedTokens, limit, "LLM_COMPRESSION"));
                    log.info("{} compressed — tokens: {} → {} (limit: {})", section, originalTokens, compressedTokens, limit);
                    return new EnforcementResult(compressed, originalTokens, compressedTokens, "LLM_COMPRESSION");
                })
                .onErrorResume(e -> {
                    log.warn("{} compression failed, falling back to truncation: {}", section, e.getMessage());
                    String truncated = truncateToTokenBudget(text, limit);
                    int truncatedTokens = tokenCounterService.countTokens(truncated);
                    budget.addAction(new TokenBudget.BudgetAction(section, originalTokens, truncatedTokens, limit, "HARD_TRUNCATION"));
                    return Mono.just(new EnforcementResult(truncated, originalTokens, truncatedTokens, "HARD_TRUNCATION"));
                });
    }

    private String truncateToTokenBudget(String text, int maxTokens) {
        if (text == null || text.isBlank()) return "";
        List<String> chunks = tokenCounterService.splitTextByTokens(text, maxTokens);
        return chunks.isEmpty() ? "" : chunks.get(0);
    }

    private com.example.demo.OptimizationRequest buildOptimizationRequest(String text, int contextWindow, TargetType targetType) {
        com.example.demo.OptimizationRequest request = new com.example.demo.OptimizationRequest();
        request.setDocument(text);
        request.setContextWindow(contextWindow);
        request.setTargetType(targetType);
        return request;
    }

    public record EnforcementResult(
            String text,
            int originalTokens,
            int finalTokens,
            String actionTaken
    ) {}
}
