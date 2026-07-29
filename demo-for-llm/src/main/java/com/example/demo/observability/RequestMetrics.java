package com.example.demo.observability;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_metrics")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String requestId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private Long latencyMs;

    @Column(nullable = false)
    private Long inputTokens;

    @Column(nullable = false)
    private Long outputTokens;

    @Column(nullable = false)
    private Boolean cacheHit;

    private Long embeddingMs;

    private Long retrievalMs;

    private Double compressionPercent;

    private Double estimatedCost;

    private Integer userRating;

    private Double complexityScore;

    @Column(length = 1000)
    private String chatId;

    @Column(length = 1000)
    private String sourceType;

    private Boolean wasOptimized;

    private Long totalTokens;

    private Long tokensSaved;

    private Double savingsPercentage;

    @Column(length = 2000)
    private String requestedProvider;

    @Column(length = 2000)
    private String executedProvider;

    private Integer contextWindow;

    private Integer remainingHeadroom;
}
