package com.example.demo.advancePlusOne;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Slf4j
public class TokenAwareLlmRouterServiceImpl implements LlmRouterService {

    private static final String DEFAULT_PROVIDER = "GEMINI";
    private static final String FALLBACK_CHEAP_PROVIDER = "FAST_TIER";

    // The threshold scale cut-off point. Anything at or above this score requires premium processing power.
    private static final double COMPLEXITY_THRESHOLD = 4.0;

    private final List<ComplexityEvaluator> evaluators;

    public TokenAwareLlmRouterServiceImpl(List<ComplexityEvaluator> evaluators) {
        this.evaluators = evaluators;
        log.info("Intelligent Cognitive Scoring Engine initialized with {} evaluators.", evaluators.size());
    }

    @Override
    public String route(ProviderRoutingContext context) {
        String requested = context.getRequestedProvider();

        // 1. Client explicitly requested a non-default tier -> Honor the request unconditionally
        if (requested != null && !requested.isBlank() && !requested.equalsIgnoreCase(DEFAULT_PROVIDER)) {
            log.debug("Explicit override detected. Routing straight to user target: {}", requested);
            return requested.toUpperCase();
        }

        // 2. Evaluate total Cognitive Load via Chain of Responsibility
        double aggregatedScore = 0.0;
        StringBuilder metricsTrace = new StringBuilder("Complexity Scoring Breakdown:\n");

        for (ComplexityEvaluator evaluator : evaluators) {
            double componentScore = evaluator.evaluate(context);
            aggregatedScore += componentScore;
            metricsTrace.append(String.format(" -> %s: %.2f\n", evaluator.getName(), componentScore));
        }

        metricsTrace.append(String.format(" ==> Total Cognitive Index: %.2f (Threshold: %.2f)", aggregatedScore, COMPLEXITY_THRESHOLD));
        log.info(metricsTrace.toString());

        // 3. Make dynamic routing choices based on computed intelligence index
        if (aggregatedScore >= COMPLEXITY_THRESHOLD) {
            log.info("Request classified as HIGH complexity. Routing to premium core: {}", DEFAULT_PROVIDER);
            return DEFAULT_PROVIDER;
        }

        log.info("Request classified as LOW complexity. Cost-optimization active. Routing to: {}", FALLBACK_CHEAP_PROVIDER);
        return FALLBACK_CHEAP_PROVIDER;
    }
}