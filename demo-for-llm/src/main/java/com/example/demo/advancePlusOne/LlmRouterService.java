package com.example.demo.advancePlusOne;

public interface LlmRouterService {
    /**
     * Determines the most optimal AI provider based on token metrics and client intent.
     */
    String route(ProviderRoutingContext context);
}