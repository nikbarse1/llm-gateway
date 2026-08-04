package com.example.demo.advancePlusOne;

import com.example.demo.*;
import com.example.demo.embeddings.GeminiEmbeddingService;
import com.example.demo.embeddings.SemanticCacheRepository;
import com.example.demo.embeddings.VectorizedHistoryService;
import com.example.demo.llmrouter.AiChatResponse;
import com.example.demo.observability.ObservabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdvancedGatewayOrchestrationService {

    private final LlmProviderRegistry providerRegistry;
    private final TokenOptimizationService tokenOptimizationService;
    private final TokenCounterService tokenCounterService;
    private final FileParserService fileParserService;
    private final WebScraperService webScraperService;
    private final LlmRouterService llmRouterService;

    // Phase 2 & 3 Services
    private final DocumentRagService documentRagService;
    private final VectorizedHistoryService vectorizedHistoryService;

    // Spring AI Native Components
    private final ChatMemory chatMemory;

    // Semantic Caching services
    private final GeminiEmbeddingService embeddingService;
    private final SemanticCacheRepository cacheRepository;

    // Token Budget Manager
    private final TokenBudgetManager tokenBudgetManager;
    private final TokenBudgetConfig tokenBudgetConfig;
    private final DynamicRagTopKResolver dynamicRagTopKResolver;

    // Prompt Template Registry
    private final PromptTemplateRegistry promptTemplateRegistry;

    // Conversation Auto-Summarization
    private final ConversationSummarizationService conversationSummarizationService;
    private final LLMSummarizationService llmSummarizationService;

    @Value("${history.recent-turns:6}")
    private int recentTurns;

    @Value("${history.summarization.min-tokens:100}")
    private int historySummarizationMinTokens;

    // Observability
    private final ObservabilityService observabilityService;
    private final TokenAwareLlmRouterServiceImpl routerServiceImpl;

    public Mono<AiChatResponse> processStatefulChat(
            String instruction, MultipartFile file, String url, String chatId,
            String providerName, int contextWindow, boolean isDevMode, String promptId) {

        long startTime = System.currentTimeMillis();
        final long[] embeddingStartTime = {0L};
        final long[] embeddingEndTime = {0L};
        final long[] retrievalStartTime = {0L};
        final long[] retrievalEndTime = {0L};
        final double[] complexityScore = {0.0};
        final boolean hasPriorTurns = chatMemory.get(chatId) != null && !chatMemory.get(chatId).isEmpty();
        final boolean hasDocumentContext = (file != null && !file.isEmpty()) || (url != null && !url.isBlank());

        Mono<ContextResult> contextMono = resolveDocumentContext(file, url);
        Mono<List<Double>> embeddingMono = (hasPriorTurns || hasDocumentContext)
                ? Mono.defer(() -> {
                    embeddingStartTime[0] = System.currentTimeMillis();
                    return embeddingService.generateEmbedding(instruction)
                            .doOnSuccess(result -> embeddingEndTime[0] = System.currentTimeMillis());
                })
                : Mono.<List<Double>>just(List.of());

        return Mono.zip(contextMono, embeddingMono).flatMap(tuple -> {
            ContextResult contextResult = tuple.getT1();
            List<Double> embedding = tuple.getT2();
            String rawContext = contextResult.text();
            
            log.info("Resolved document context - sourceType: {}, rawContextLength: {}",
                    contextResult.sourceType(), rawContext.length());

            return Mono.fromCallable(() -> cacheRepository.findCachedResponse(embedding, rawContext))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(cachedAnswer -> {
                        if (cachedAnswer != null) {
                            log.info("⚡ Returning instant response from Context-Aware Semantic Cache for chatId: {}", chatId);
                            long latency = System.currentTimeMillis() - startTime;
                        Long embeddingMs = embeddingEndTime[0] > 0 ? embeddingEndTime[0] - embeddingStartTime[0] : null;
                        return handleCacheHit(instruction, cachedAnswer, chatId, isDevMode, startTime, embeddingMs);
                        }
                        retrievalStartTime[0] = 0L;
                        retrievalEndTime[0] = 0L;
                        return executeFullLlmPipeline(
                                instruction, rawContext, contextResult.sourceType(), chatId,
                                providerName, contextWindow, isDevMode, embedding, hasPriorTurns, promptId,
                                startTime, embeddingStartTime[0], embeddingEndTime[0], retrievalStartTime, retrievalEndTime, complexityScore
                        );
                    })
                    .switchIfEmpty(executeFullLlmPipeline(
                            instruction, rawContext, contextResult.sourceType(), chatId,
                            providerName, contextWindow, isDevMode, embedding, hasPriorTurns, promptId,
                            startTime, embeddingStartTime[0], embeddingEndTime[0], retrievalStartTime, retrievalEndTime, complexityScore
                    ));
        });
    }

    private Mono<AiChatResponse> executeFullLlmPipeline(
            String instruction, String rawContext, String sourceType, String chatId,
            String providerName, int contextWindow, boolean isDevMode,
            List<Double> instructionEmbedding, boolean hasPriorTurns, String promptId,
            long startTime, long embeddingStartTime, long embeddingEndTime,
            long[] retrievalStartTime, long[] retrievalEndTime, double[] complexityScore) {

        TokenBudget budget = tokenBudgetManager.createBudget();
        int instructionTokens = tokenCounterService.countTokens(instruction);
        boolean hasHeavyContext = !rawContext.isBlank();
        int dynamicTopK = dynamicRagTopKResolver.resolveTopK(instruction, instructionTokens, hasHeavyContext);

        Mono<Void> ragIndexMono = (!rawContext.isBlank())
                ? documentRagService.indexDocument(chatId, rawContext)
                : Mono.empty();

        Mono<List<String>> ragChunksMono = ragIndexMono.then(
                (!rawContext.isBlank())
                        ? Mono.defer(() -> {
                            retrievalStartTime[0] = System.currentTimeMillis();
                            return documentRagService.retrieveRelevantChunks(chatId, instruction, dynamicTopK)
                                    .doOnSuccess(result -> retrievalEndTime[0] = System.currentTimeMillis());
                        })
                        : Mono.just(List.of())
        );

        Mono<String> relevantHistoryMono = hasPriorTurns
                ? vectorizedHistoryService.retrieveRelevantHistory(chatId, instruction, 5)
                : Mono.just("");

        return Mono.zip(ragChunksMono, relevantHistoryMono, buildOptimizedHistory(chatId)).flatMap(tuple -> {
                List<String> ragChunks = tuple.getT1();
                String semanticHistorySnippet = tuple.getT2();
                HistoryResult historyResult = tuple.getT3();

                // Join RAG chunks into a single context string
                String ragContextSnippet = ragChunks.stream()
                        .collect(Collectors.joining("\n\n--- [Retrieved Context Chunk] ---\n"));

                // Step 6: Enforce token budgets on History and RAG in parallel
                Mono<TokenBudgetManager.EnforcementResult> historyEnforcement =
                        tokenBudgetManager.enforceHistoryBudget(semanticHistorySnippet, budget);
                Mono<TokenBudgetManager.EnforcementResult> ragEnforcement =
                        tokenBudgetManager.enforceRagBudget(ragContextSnippet, budget);

                return Mono.zip(historyEnforcement, ragEnforcement).flatMap(enfTuple -> {
                    TokenBudgetManager.EnforcementResult enforcedHistory = enfTuple.getT1();
                    TokenBudgetManager.EnforcementResult enforcedRag = enfTuple.getT2();

                    // Build augmented instruction with budget-enforced RAG context
                    String augmentedInstruction = buildAugmentedInstruction(instruction, enforcedRag.text());
                    log.info("Augmented instruction - chatId: {}, length: {}, preview: '{}'",
                            chatId, augmentedInstruction.length(), truncate(augmentedInstruction, 300));

                    // Step 7: Enforce prompt budget (compress if user+RAG exceeds user reserve)
                    return tokenBudgetManager.enforcePromptBudget(augmentedInstruction, budget).flatMap(enforcedPrompt -> {

                        boolean wasHistoryOptimized = historyResult.summarizedHistoryTokens() < historyResult.rawHistoryTokens();
                        boolean wasOptimized = wasHistoryOptimized || budget.getActions().stream()
                                .anyMatch(a -> !a.actionTaken().equals("PASS_THROUGH"));

                        // Step 8: Construct Structured Spring AI Prompt
                        // Resolve the system prompt from the template registry (by promptId) or use default
                        List<Message> messagesToSend = new ArrayList<>();
                        PromptTemplate template = promptTemplateRegistry.resolveOrDefault(promptId);
                        String systemRules = template.getContent();
                        log.info("Resolved system prompt — promptId: {}, template: {}, tokens: {}",
                                promptId != null ? promptId : "default", template.getName(), template.getTokenCount());
                        if (enforcedHistory.text() != null && !enforcedHistory.text().isBlank()) {
                            systemRules += "\n\n=== RELEVANT CONVERSATION HISTORY ===\n" + enforcedHistory.text();
                        }
                        messagesToSend.add(new SystemMessage(systemRules));

                        // Add summarized history (older turns) plus the last N raw turns
                        messagesToSend.addAll(historyResult.optimizedHistoryMessages());

                        messagesToSend.add(new UserMessage(enforcedPrompt.text()));

                        Prompt compiledPrompt = new Prompt(messagesToSend);
                        log.info("Compiled prompt - chatId: {}, messages: {}, promptPreview: '{}'",
                                chatId, compiledPrompt.getInstructions().size(), truncate(compiledPrompt.getInstructions().toString(), 500));

                        int heuristicPromptTokens = tokenCounterService.countTokens(compiledPrompt.getInstructions().toString());

                        // Raw baseline: what the prompt would look like without any history/RAG/user optimization
                        String rawSystemRules = template.getContent()
                                + (semanticHistorySnippet != null && !semanticHistorySnippet.isBlank()
                                        ? "\n\n=== RELEVANT CONVERSATION HISTORY ===\n" + semanticHistorySnippet
                                        : "");
                        String rawHistoryText = historyResult.rawHistoryText();
                        String rawUserPrompt = buildAugmentedInstruction(instruction, ragContextSnippet);

                        ProviderRoutingContext routingContext = ProviderRoutingContext.builder()
                                .requestedProvider(providerName)
                                .instruction(instruction)
                                .instructionTokens(enforcedPrompt.finalTokens())
                                .finalPromptTokens(heuristicPromptTokens)
                                .hasHeavyContext(!rawContext.isBlank())
                                .build();

                        String smartProviderName = llmRouterService.route(routingContext);
                        final String[] executedProvider = { smartProviderName };
                        
                        if (routerServiceImpl != null) {
                            double score = 0.0;
                            for (var evaluator : List.of()) {
                            }
                            complexityScore[0] = score;
                        }
                        LlmProvider targetLlm = providerRegistry.getProvider(smartProviderName);

                        // Step 9: Execute LLM Completion (fallback to GEMINI if the primary provider fails)
                        return targetLlm.askAi(compiledPrompt)
                                .doOnError(e -> log.warn("Provider {} failed: {}", smartProviderName, e.getMessage()))
                                .onErrorResume(e -> {
                                    if (!"GEMINI".equalsIgnoreCase(smartProviderName)) {
                                        log.warn("Falling back from {} to GEMINI due to error: {}", smartProviderName, e.getMessage());
                                        executedProvider[0] = "GEMINI";
                                        LlmProvider fallbackLlm = providerRegistry.getProvider("GEMINI");
                                        return fallbackLlm.askAi(compiledPrompt)
                                                .doOnError(fallbackError -> log.error("GEMINI fallback also failed: {}", fallbackError.getMessage()));
                                    }
                                    return Mono.error(e);
                                })
                                .flatMap(chatResponse -> {
                                    log.info("Received chat response - provider: {}, chatId: {}", executedProvider[0], chatId);
                                    String aiAnswerText = (chatResponse.getResult() != null && chatResponse.getResult().getOutput() != null && chatResponse.getResult().getOutput().getText() != null)
                                            ? chatResponse.getResult().getOutput().getText()
                                            : "";
                                    org.springframework.ai.chat.metadata.Usage actualUsage = (chatResponse.getMetadata() != null)
                                            ? chatResponse.getMetadata().getUsage()
                                            : null;
                                    log.info("AI answer - chatId: {}, answerLength: {}, usage: {}",
                                            chatId, aiAnswerText.length(), actualUsage);

                                    // Cache hit saving in background (Context-Aware)
                                    if (instructionEmbedding != null && !instructionEmbedding.isEmpty()) {
                                        Mono.fromRunnable(() -> cacheRepository.cacheResponse(instruction, aiAnswerText, instructionEmbedding, rawContext))
                                                .subscribeOn(Schedulers.boundedElastic()).subscribe();
                                    }

                                    // Step 10: Save turns to Spring AI ChatMemory immediately; persist to vector history and summarize in the background
                                    chatMemory.add(chatId, List.of(new UserMessage(instruction), new AssistantMessage(aiAnswerText)));

                                    vectorizedHistoryService.saveMessageToHistory(chatId, "USER", instruction)
                                            .onErrorResume(e -> { log.warn("Background user history save failed: {}", e.getMessage()); return Mono.<Void>empty(); })
                                            .subscribe(v -> {}, e -> {});
                                    vectorizedHistoryService.saveMessageToHistory(chatId, "ASSISTANT", aiAnswerText)
                                            .onErrorResume(e -> { log.warn("Background assistant history save failed: {}", e.getMessage()); return Mono.<Void>empty(); })
                                            .subscribe(v -> {}, e -> {});
                                    conversationSummarizationService.maybeSummarize(chatId)
                                            .onErrorResume(e -> { log.warn("Background summarization failed: {}", e.getMessage()); return Mono.<Void>empty(); })
                                            .subscribe(v -> {}, e -> {});

                                    return Mono.defer(() -> {

                                        int hypotheticalRawTokens = tokenCounterService.countTokens(
                                                rawSystemRules + "\n" + rawHistoryText + "\n" + rawUserPrompt,
                                                executedProvider[0]);

                                        OptimizationResponse.HistoryOptimization historyOptimization = OptimizationResponse.HistoryOptimization.builder()
                                                .rawHistoryTokens(historyResult.rawHistoryTokens())
                                                .summarizedHistoryTokens(historyResult.summarizedHistoryTokens())
                                                .historyTokensSaved(Math.max(0, historyResult.rawHistoryTokens() - historyResult.summarizedHistoryTokens()))
                                                .build();

                                        OptimizationResponse mergedMetrics = mergeMetrics(
                                                contextWindow,
                                                compiledPrompt.getInstructions().toString(), actualUsage,
                                                hypotheticalRawTokens, executedProvider[0], providerName,
                                                budget, historyOptimization
                                        );

                                        boolean finalWasOptimized = wasOptimized
                                                || (mergedMetrics.getUsageMetrics() != null && mergedMetrics.getUsageMetrics().getTokensSaved() > 0)
                                                || (mergedMetrics.getHistoryOptimization() != null && mergedMetrics.getHistoryOptimization().getHistoryTokensSaved() > 0);

                                        AiChatResponse response = AiChatResponse.builder()
                                                .userReadableMessage(aiAnswerText)
                                                .sourceType(sourceType)
                                                .wasOptimized(finalWasOptimized)
                                                .optimizationMetrics(isDevMode ? mergedMetrics : null)
                                                .chatId(chatId)
                                                .build();

                                        log.info("Returning AiChatResponse - chatId: {}, sourceType: {}, wasOptimized: {}, responseLength: {}",
                                                chatId, response.getSourceType(), response.isWasOptimized(),
                                                response.getUserReadableMessage() != null ? response.getUserReadableMessage().length() : 0);
                                        
                                        long totalLatency = System.currentTimeMillis() - startTime;
                                        Long embeddingMs = embeddingEndTime > 0 ? embeddingEndTime - embeddingStartTime : null;
                                        Long retrievalMs = retrievalEndTime[0] > 0 ? retrievalEndTime[0] - retrievalStartTime[0] : null;
                                        
                                        double compressionPercent = 0.0;
                                        if (mergedMetrics != null && mergedMetrics.getUsageMetrics() != null) {
                                            compressionPercent = mergedMetrics.getUsageMetrics().getSavingsPercentage();
                                        }
                                        
                                        long promptTokenCount = actualUsage != null ? actualUsage.getPromptTokens() : 0L;
                                        long completionTokenCount = actualUsage != null ? actualUsage.getCompletionTokens() : 0L;
                                        double estimatedCost = calculateEstimatedCost(
                                            promptTokenCount,
                                            completionTokenCount,
                                            executedProvider[0]
                                        );
                                        
                                        observabilityService.captureAndPersistMetrics(
                                            chatId,
                                            executedProvider[0],
                                            totalLatency,
                                            promptTokenCount,
                                            completionTokenCount,
                                            false,
                                            embeddingMs,
                                            retrievalMs,
                                            compressionPercent,
                                            estimatedCost,
                                            complexityScore[0],
                                            response
                                        );
                                        
                                        return Mono.just(response);
                                    });
                                });
                    });
                });
        });
    }

    private Mono<AiChatResponse> handleCacheHit(String instruction, String cachedAnswer, String chatId, boolean isDevMode, long startTime, Long embeddingMs) {
        chatMemory.add(chatId, List.of(new UserMessage(instruction), new AssistantMessage(cachedAnswer)));
        log.info("Returning cached AiChatResponse - chatId: {}, responseLength: {}",
                chatId, cachedAnswer != null ? cachedAnswer.length() : 0);

        return vectorizedHistoryService.saveMessageToHistory(chatId, "USER", instruction)
                .then(vectorizedHistoryService.saveMessageToHistory(chatId, "ASSISTANT", cachedAnswer))
                .then(Mono.defer(() -> {
                    AiChatResponse response = AiChatResponse.builder()
                            .userReadableMessage(cachedAnswer)
                            .sourceType("SEMANTIC_CACHE_HIT")
                            .wasOptimized(true)
                            .chatId(chatId)
                            .build();
                    
                    long totalLatency = System.currentTimeMillis() - startTime;
                    int outputTokens = tokenCounterService.countTokens(cachedAnswer);
                    
                    observabilityService.captureAndPersistMetrics(
                        chatId,
                        "CACHE",
                        totalLatency,
                        0L,
                        (long) outputTokens,
                        true,
                        embeddingMs,
                        null,
                        null,
                        0.0,
                        null,
                        response
                    );
                    
                    return Mono.just(response);
                }));
    }

    private OptimizationResponse mergeMetrics(
            int contextWindow,
            String finalPromptContent, org.springframework.ai.chat.metadata.Usage actualUsage,
            int hypotheticalRawTokens, String actualProvider, String requestedProvider,
            TokenBudget budget, OptimizationResponse.HistoryOptimization historyOptimization) {

        long promptTokens = actualUsage != null ? actualUsage.getPromptTokens() : 0L;
        long completionTokens = actualUsage != null ? actualUsage.getCompletionTokens() : 0L;
        long totalTokens = actualUsage != null ? actualUsage.getTotalTokens() : 0L;

        long tokensSaved = promptTokens > 0 ? Math.max(0, hypotheticalRawTokens - promptTokens) : 0L;
        double savingsPercent = promptTokens > 0 && hypotheticalRawTokens > 0 ? ((double) tokensSaved / hypotheticalRawTokens) * 100 : 0.0;

        return OptimizationResponse.builder()
                .routingDecision(OptimizationResponse.RoutingDecision.builder()
                        .requestedProvider(requestedProvider)
                        .executedProvider(actualProvider)
                        .build())
                .usageMetrics(OptimizationResponse.UsageMetrics.builder()
                        .unoptimizedPromptTokens(hypotheticalRawTokens)
                        .optimizedPromptTokens(promptTokens)
                        .actualPromptTokens(promptTokens)
                        .actualCompletionTokens(completionTokens)
                        .actualTotalTokens(totalTokens)
                        .tokensSaved(tokensSaved)
                        .savingsPercentage(Double.parseDouble(String.format("%.2f", savingsPercent)))
                        .build())
                .payloadSnapshot(OptimizationResponse.PayloadSnapshot.builder()
                        .contextWindowSize(contextWindow)
                        .remainingHeadroom(Math.max(0, contextWindow - (int) totalTokens))
                        .finalPrompt(finalPromptContent)
                        .build())
                .budgetAllocation(buildBudgetAllocation(budget))
                .historyOptimization(historyOptimization)
                .build();
    }

    private OptimizationResponse.BudgetAllocation buildBudgetAllocation(TokenBudget budget) {
        return OptimizationResponse.BudgetAllocation.builder()
                .totalBudget(budget.getTotalBudget())
                .systemReserve(budget.getSystemReserve())
                .historyReserve(budget.getHistoryReserve())
                .ragReserve(budget.getRagReserve())
                .userReserve(budget.getUserReserve())
                .responseReserve(budget.getResponseReserve())
                .actions(budget.getActions().stream()
                        .map(a -> OptimizationResponse.BudgetActionDetail.builder()
                                .section(a.section())
                                .requestedTokens(a.requestedTokens())
                                .allocatedTokens(a.allocatedTokens())
                                .budgetLimit(a.budgetLimit())
                                .actionTaken(a.actionTaken())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private Mono<ContextResult> resolveDocumentContext(MultipartFile file, String url) {
        if (file != null && !file.isEmpty()) {
            return Mono.fromCallable(() -> {
                String text = fileParserService.extractText(file);
                log.info("Extracted text from file: {} chars, will be intelligently chunked during RAG indexing", text.length());
                return text;
            })
                    .subscribeOn(Schedulers.boundedElastic())
                    .map(text -> new ContextResult(text, "FILE"))
                    .onErrorResume(e -> Mono.just(new ContextResult("", "FILE_ERROR")));
        }
        if (url != null && !url.isBlank()) {
            return webScraperService.scrapeUrl(url)
                    .map(result -> new ContextResult(result.content(), "URL"));
        }
        return Mono.just(new ContextResult("", "TEXT_ONLY"));
    }

    private record HistoryResult(
            String rawHistoryText,
            List<Message> optimizedHistoryMessages,
            int rawHistoryTokens,
            int summarizedHistoryTokens
    ) {}

    private Mono<HistoryResult> buildOptimizedHistory(String chatId) {
        List<Message> shortTermMemory = chatMemory.get(chatId);
        if (shortTermMemory == null || shortTermMemory.isEmpty()) {
            return Mono.just(new HistoryResult("", List.of(), 0, 0));
        }

        String rawHistoryText = shortTermMemory.stream()
                .map(Message::getText)
                .collect(Collectors.joining("\n"));
        int rawHistoryTokens = tokenCounterService.countTokens(rawHistoryText);

        int recentMessageCount = recentTurns * 2;
        if (shortTermMemory.size() <= recentMessageCount) {
            String summarizedText = shortTermMemory.stream()
                    .map(Message::getText)
                    .collect(Collectors.joining("\n"));
            int summarizedTokens = tokenCounterService.countTokens(summarizedText);
            return Mono.just(new HistoryResult(rawHistoryText, new ArrayList<>(shortTermMemory), rawHistoryTokens, summarizedTokens));
        }

        List<Message> older = shortTermMemory.subList(0, shortTermMemory.size() - recentMessageCount);
        List<Message> recent = new ArrayList<>(shortTermMemory.subList(shortTermMemory.size() - recentMessageCount, shortTermMemory.size()));

        String olderText = older.stream()
                .filter(m -> m.getText() != null && !m.getText().contains("```"))
                .map(Message::getText)
                .collect(Collectors.joining("\n"));
        int olderTokens = tokenCounterService.countTokens(olderText);

        if (olderTokens < historySummarizationMinTokens || olderText.isBlank()) {
            return Mono.just(new HistoryResult(rawHistoryText, new ArrayList<>(shortTermMemory), rawHistoryTokens, rawHistoryTokens));
        }

        return llmSummarizationService.smartCompress(olderText, OptimizationRequest.TargetType.HISTORY)
                .map(summary -> {
                    String summarizedHistory = "Earlier conversation summary:\n" + summary;
                    List<Message> optimized = new ArrayList<>();
                    optimized.add(new SystemMessage(summarizedHistory));
                    optimized.addAll(recent);

                    String summarizedHistoryText = optimized.stream()
                            .map(Message::getText)
                            .collect(Collectors.joining("\n"));
                    int summarizedHistoryTokens = tokenCounterService.countTokens(summarizedHistoryText);
                    return new HistoryResult(rawHistoryText, optimized, rawHistoryTokens, summarizedHistoryTokens);
                })
                .onErrorResume(e -> {
                    log.warn("History summarization failed, falling back to full history: {}", e.getMessage());
                    return Mono.just(new HistoryResult(rawHistoryText, new ArrayList<>(shortTermMemory), rawHistoryTokens, rawHistoryTokens));
                });
    }

    private String buildAugmentedInstruction(String instruction, String ragSnippet) {
        if (ragSnippet == null || ragSnippet.isBlank()) return instruction;
        return String.format("%s\n\n--- Relevant Document Context ---\n%s", instruction, ragSnippet);
    }

    private static String truncate(String text, int max) {
        if (text == null || text.length() <= max) return text;
        return text.substring(0, max) + "...";
    }

    private double calculateEstimatedCost(long promptTokens, long completionTokens, String provider) {
        double inputCostPer1M = 0.0;
        double outputCostPer1M = 0.0;
        
        switch (provider.toUpperCase()) {
            case "GEMINI":
                inputCostPer1M = 0.075;
                outputCostPer1M = 0.30;
                break;
            case "FAST_TIER":
                inputCostPer1M = 0.15;
                outputCostPer1M = 0.60;
                break;
            case "CACHE":
                return 0.0;
            default:
                inputCostPer1M = 0.10;
                outputCostPer1M = 0.40;
        }
        
        double inputCost = (promptTokens / 1_000_000.0) * inputCostPer1M;
        double outputCost = (completionTokens / 1_000_000.0) * outputCostPer1M;
        
        return inputCost + outputCost;
    }

    private record ContextResult(String text, String sourceType) {}
}