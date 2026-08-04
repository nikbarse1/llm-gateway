package com.example.demo.advancePlusOne;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProviderRoutingContext {
    String requestedProvider;
    String instruction;
    int instructionTokens;
    int finalPromptTokens;
    boolean hasHeavyContext;
}