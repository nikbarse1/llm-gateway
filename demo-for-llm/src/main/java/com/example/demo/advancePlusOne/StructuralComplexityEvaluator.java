package com.example.demo.advancePlusOne;

import org.springframework.stereotype.Component;

@Component
public class StructuralComplexityEvaluator implements ComplexityEvaluator {

    @Override
    public double evaluate(ProviderRoutingContext context) {
        String instruction = context.getRequestedProvider(); // Using raw instruction or incoming text payload references
        if (instruction == null || instruction.isBlank()) {
            return 0.0;
        }

        double score = 0.0;

        // Code snippet block markers
        if (instruction.contains("```") || instruction.contains("`")) {
            score += 3.0;
        }

        // Structural payloads (JSON, XML, Key-Value configs)
        if ((instruction.contains("{") && instruction.contains("}")) ||
                (instruction.contains("<") && instruction.contains("/>"))) {
            score += 2.5;
        }

        // Algorithmic/Mathematical operators or arrow assignments
        if (instruction.contains("->") || instruction.contains("=>") || instruction.contains("==")) {
            score += 1.5;
        }

        return score;
    }

    @Override
    public String getName() {
        return "StructuralSignatureEvaluator";
    }
}