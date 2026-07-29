package com.example.demo.advancePlusOne;

public interface ComplexityEvaluator {
    /**
     * Inspects the payload context and returns a complexity score weight.
     * Higher values imply higher cognitive complexity.
     */
    double evaluate(ProviderRoutingContext context);

    /**
     * Provides a descriptive name of the evaluator for logging and debugging metrics.
     */
    String getName();
}