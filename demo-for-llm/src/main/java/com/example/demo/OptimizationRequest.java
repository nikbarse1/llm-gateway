package com.example.demo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request for optimizing a document to fit within a context window")
public class OptimizationRequest {
    
    @NotBlank(message = "Document is required")
    @Schema(description = "The document text to optimize", example = "Long document text here...")
    private String document;
    
    @Min(value = 100, message = "Context window must be at least 100 tokens")
    @Schema(description = "Target context window size in tokens", example = "16000", defaultValue = "16000")
    private int contextWindow = 16000;

    private TargetType targetType; // <-- New field

    public enum TargetType {
        INSTRUCTION,
        DOCUMENT,
        HISTORY
    }
}
