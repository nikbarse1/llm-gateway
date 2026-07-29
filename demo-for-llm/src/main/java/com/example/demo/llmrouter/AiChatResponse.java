package com.example.demo.llmrouter;

import com.example.demo.OptimizationResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {
    private String userReadableMessage;
    private String sourceType;
    private boolean wasOptimized;
    private OptimizationResponse optimizationMetrics;
    private String chatId;
}