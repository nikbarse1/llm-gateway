package com.example.demo.advancePlusOne;

import org.springframework.stereotype.Component;

@Component
public class ContextualDepthEvaluator implements ComplexityEvaluator {

    @Override
    public double evaluate(ProviderRoutingContext context) {
        double score = 0.0;

        // Account for total compiled string weight (history + document attachments)
        if (context.getFinalPromptTokens() > 2000) {
            score += 2.0;
        } else if (context.getFinalPromptTokens() > 1000) {
            score += 1.0;
        }

        // Heavy context payload triggers an explicit baseline cost increase
        if (context.isHasHeavyContext()) {
            score += 2.5;
        }

        return score;
    }

    @Override
    public String getName() {
        return "ContextualDepthEvaluator";
    }
}