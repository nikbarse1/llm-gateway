package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedAnalysisResponse {
    
    public enum InputType {
        TEXT, FILE, URL
    }
    
    public enum OptimizationLevel {
        NONE, LIGHT, MEDIUM, AGGRESSIVE;

        /**
         * Automatically classifies the compression level actually applied, based on the
         * measured token reduction. The caller never selects this - it is decided by the
         * optimization pipeline depending on how much compression the document needed to
         * fit the requested context window.
         */
        public static OptimizationLevel fromReductionPercentage(double reductionPercentage) {
            if (reductionPercentage <= 0.5) {
                return NONE;
            } else if (reductionPercentage < 30) {
                return LIGHT;
            } else if (reductionPercentage < 60) {
                return MEDIUM;
            }
            return AGGRESSIVE;
        }
    }
    
    private InputType inputType;
    private OptimizationLevel optimizationLevel;
    private String source;
    private String contentType;
    private String extractionMethod;
    
    // Token optimization metrics
    private int originalTokens;
    private int optimizedTokens;
    private double reductionPercentage;
    private int contextWindow;
    private int headroomBefore;
    private int headroomAfter;
    
    // Content
    private String optimizedContent;
    private String originalContentPreview; // First 200 chars
    
    // Metadata
    private LocalDateTime timestamp;
    private boolean successful;
    private String errorMessage;
    
    // Additional metadata based on input type
    private FileMetadata fileMetadata;
    private UrlMetadata urlMetadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileMetadata {
        private String filename;
        private long fileSize;
        private String fileExtension;
        private String detectedMimeType;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UrlMetadata {
        private String originalUrl;
        private String finalUrl; // After redirects
        private String domain;
        private String title;
        private int responseTime;
        private boolean isAccessible;
    }
}
