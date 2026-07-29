package com.example.demo.observability;

import com.example.demo.OptimizationResponse;
import com.example.demo.llmrouter.AiChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ObservabilityService {

    private final RequestMetricsRepository metricsRepository;

    public String captureAndPersistMetrics(
            String chatId,
            String provider,
            long latencyMs,
            long inputTokens,
            long outputTokens,
            boolean cacheHit,
            Long embeddingMs,
            Long retrievalMs,
            Double compressionPercent,
            Double estimatedCost,
            Double complexityScore,
            AiChatResponse response) {

        String requestId = UUID.randomUUID().toString();

        RequestMetrics metrics = RequestMetrics.builder()
                .requestId(requestId)
                .timestamp(LocalDateTime.now())
                .provider(provider)
                .latencyMs(latencyMs)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .cacheHit(cacheHit)
                .embeddingMs(embeddingMs)
                .retrievalMs(retrievalMs)
                .compressionPercent(compressionPercent)
                .estimatedCost(estimatedCost)
                .complexityScore(complexityScore)
                .chatId(chatId)
                .sourceType(response != null ? response.getSourceType() : null)
                .wasOptimized(response != null ? response.isWasOptimized() : false)
                .build();

        if (response != null && response.getOptimizationMetrics() != null) {
            OptimizationResponse optMetrics = response.getOptimizationMetrics();
            
            if (optMetrics.getUsageMetrics() != null) {
                metrics.setTotalTokens(optMetrics.getUsageMetrics().getActualTotalTokens());
                metrics.setTokensSaved(optMetrics.getUsageMetrics().getTokensSaved());
                metrics.setSavingsPercentage(optMetrics.getUsageMetrics().getSavingsPercentage());
            }
            
            if (optMetrics.getRoutingDecision() != null) {
                metrics.setRequestedProvider(optMetrics.getRoutingDecision().getRequestedProvider());
                metrics.setExecutedProvider(optMetrics.getRoutingDecision().getExecutedProvider());
            }
            
            if (optMetrics.getPayloadSnapshot() != null) {
                metrics.setContextWindow(optMetrics.getPayloadSnapshot().getContextWindowSize());
                metrics.setRemainingHeadroom(optMetrics.getPayloadSnapshot().getRemainingHeadroom());
            }
        }

        try {
            metricsRepository.save(metrics);
            log.info("Persisted request metrics - requestId: {}, provider: {}, latency: {}ms, tokens: {}/{}, cacheHit: {}",
                    requestId, provider, latencyMs, inputTokens, outputTokens, cacheHit);
        } catch (Exception e) {
            log.error("Failed to persist request metrics for requestId: {}", requestId, e);
        }

        return requestId;
    }

    public void updateUserRating(String requestId, Integer rating) {
        try {
            metricsRepository.findByRequestId(requestId).ifPresent(metrics -> {
                metrics.setUserRating(rating);
                metricsRepository.save(metrics);
                log.info("Updated user rating for requestId: {} to {}", requestId, rating);
            });
        } catch (Exception e) {
            log.error("Failed to update user rating for requestId: {}", requestId, e);
        }
    }

    public RequestMetrics getMetricsByRequestId(String requestId) {
        return metricsRepository.findByRequestId(requestId).orElse(null);
    }
}
