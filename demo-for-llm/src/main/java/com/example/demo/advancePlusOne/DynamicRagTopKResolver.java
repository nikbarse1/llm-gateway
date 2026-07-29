package com.example.demo.advancePlusOne;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class DynamicRagTopKResolver {

    private final List<ComplexityEvaluator> evaluators;
    private final TokenBudgetConfig config;

    private static final double SIMPLE_THRESHOLD = 2.0;
    private static final double MEDIUM_THRESHOLD = 4.0;
    private static final double COMPLEX_THRESHOLD = 8.0;

    public int resolveTopK(String instruction, int instructionTokens, boolean hasHeavyContext) {
        ProviderRoutingContext context = ProviderRoutingContext.builder()
                .requestedProvider(instruction)
                .instructionTokens(instructionTokens)
                .finalPromptTokens(instructionTokens)
                .hasHeavyContext(hasHeavyContext)
                .build();

        double aggregatedScore = 0.0;
        for (ComplexityEvaluator evaluator : evaluators) {
            aggregatedScore += evaluator.evaluate(context);
        }

        int topK = mapScoreToTopK(aggregatedScore);
        log.info("Dynamic RAG topK resolved — complexityScore: {} → topK: {} ({})",
                String.format("%.2f", aggregatedScore), topK, classify(aggregatedScore));
        return topK;
    }

    private int mapScoreToTopK(double score) {
        if (score >= COMPLEX_THRESHOLD) return config.getRagTopKVeryComplex();
        if (score >= MEDIUM_THRESHOLD) return config.getRagTopKComplex();
        if (score >= SIMPLE_THRESHOLD) return config.getRagTopKMedium();
        return config.getRagTopKSimple();
    }

    private String classify(double score) {
        if (score >= COMPLEX_THRESHOLD) return "VERY_COMPLEX";
        if (score >= MEDIUM_THRESHOLD) return "COMPLEX";
        if (score >= SIMPLE_THRESHOLD) return "MEDIUM";
        return "SIMPLE";
    }
}
