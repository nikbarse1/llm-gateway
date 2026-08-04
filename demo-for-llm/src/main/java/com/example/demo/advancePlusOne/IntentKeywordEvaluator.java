package com.example.demo.advancePlusOne;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class IntentKeywordEvaluator implements ComplexityEvaluator {

    // Weights representing cognitive intensity
    private static final Map<String, Double> COGNITIVE_WEIGHTS = Map.ofEntries(
            // High cognitive requirements
            Map.entry("architect", 4.0),
            Map.entry("refactor", 3.5),
            Map.entry("optimize", 3.5),
            Map.entry("debug", 3.0),
            Map.entry("troubleshoot", 3.0),
            Map.entry("calculate", 2.5),
            Map.entry("analyze", 2.0),

            // Low cognitive tasks (negative scores lower overall weight)
            Map.entry("summarize", -2.0),
            Map.entry("translate", -1.5),
            Map.entry("fix grammar", -2.0),
            Map.entry("format", -1.5)
    );

    @Override
    public double evaluate(ProviderRoutingContext context) {
        String input = context.getInstruction();
        if (input == null || input.isBlank()) {
            return 0.0;
        }

        String lowerInput = input.toLowerCase();

        return COGNITIVE_WEIGHTS.entrySet().stream()
                .filter(entry -> lowerInput.contains(entry.getKey()))
                .mapToDouble(Map.Entry::getValue)
                .sum();
    }

    @Override
    public String getName() {
        return "IntentKeywordEvaluator";
    }
}