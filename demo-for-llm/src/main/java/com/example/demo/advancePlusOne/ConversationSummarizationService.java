package com.example.demo.advancePlusOne;

import com.example.demo.LLMSummarizationService;
import com.example.demo.OptimizationRequest;
import com.example.demo.embeddings.VectorizedHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationSummarizationService {

    private final VectorizedHistoryService vectorizedHistoryService;
    private final LLMSummarizationService llmSummarizationService;

    @Value("${conversation.summarization.threshold:200}")
    private int summarizationThreshold;

    /**
     * After each conversation turn is saved, checks if the message count for this chatId
     * has exceeded the threshold. If so, retrieves all history, summarizes it into a compact
     * key-value ledger, and stores the summary as an additional SUMMARY entry.
     *
     * Original messages are NOT deleted — they remain available for semantic retrieval.
     * The summary serves as a compact fallback that the budget manager can use when
     * the full history exceeds the token reserve.
     */
    public Mono<Void> maybeSummarize(String chatId) {
        return Mono.fromCallable(() -> vectorizedHistoryService.countMessagesByChatId(chatId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(messageCount -> {
                    if (messageCount < summarizationThreshold) {
                        log.debug("History check — chatId: {}, messages: {}/{} (no summarization needed)",
                                chatId, messageCount, summarizationThreshold);
                        return Mono.empty();
                    }

                    log.info("History threshold exceeded — chatId: {}, messages: {} → triggering auto-summarization",
                            chatId, messageCount);

                    return performSummarization(chatId, messageCount);
                })
                .onErrorResume(e -> {
                    log.error("Auto-summarization failed for chatId={}: {}", chatId, e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> performSummarization(String chatId, int originalMessageCount) {
        return Mono.fromCallable(() -> vectorizedHistoryService.retrieveAllHistoryByChatId(chatId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(fullHistory -> {
                    if (fullHistory == null || fullHistory.isBlank()) {
                        log.warn("No history content to summarize for chatId={}", chatId);
                        return Mono.empty();
                    }

                    log.info("Summarizing {} characters of conversation history for chatId={}",
                            fullHistory.length(), chatId);

                    return llmSummarizationService.smartCompress(fullHistory, OptimizationRequest.TargetType.HISTORY)
                            .flatMap(summary -> {
                                log.info("History summarized for chatId={} — stored compact summary alongside {} original messages (nothing deleted)",
                                        chatId, originalMessageCount);

                                // Store the summary as an additional SUMMARY entry
                                // Original messages remain for semantic retrieval — no deletion
                                return vectorizedHistoryService.saveMessageToHistory(chatId, "SUMMARY", summary);
                            });
                })
                .doOnSuccess(v -> log.info("Auto-summarization completed for chatId={}", chatId))
                .doOnError(e -> log.error("Auto-summarization error for chatId={}: {}", chatId, e.getMessage()))
                .then();
    }
}
