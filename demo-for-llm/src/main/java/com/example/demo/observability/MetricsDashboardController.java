package com.example.demo.observability;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/observability")
@Slf4j
@RequiredArgsConstructor
public class MetricsDashboardController {

    private final RequestMetricsRepository metricsRepository;
    private final ObservabilityService observabilityService;

    @GetMapping("/metrics")
    public ResponseEntity<List<RequestMetrics>> getAllMetrics() {
        log.info("Fetching all request metrics");
        List<RequestMetrics> metrics = metricsRepository.findAllOrderByTimestampDesc();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/metrics/{requestId}")
    public ResponseEntity<RequestMetrics> getMetricsByRequestId(@PathVariable String requestId) {
        log.info("Fetching metrics for requestId: {}", requestId);
        RequestMetrics metrics = observabilityService.getMetricsByRequestId(requestId);
        if (metrics != null) {
            return ResponseEntity.ok(metrics);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/metrics/chat/{chatId}")
    public ResponseEntity<List<RequestMetrics>> getMetricsByChatId(@PathVariable String chatId) {
        log.info("Fetching metrics for chatId: {}", chatId);
        List<RequestMetrics> metrics = metricsRepository.findByChatId(chatId);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/metrics/provider/{provider}")
    public ResponseEntity<List<RequestMetrics>> getMetricsByProvider(@PathVariable String provider) {
        log.info("Fetching metrics for provider: {}", provider);
        List<RequestMetrics> metrics = metricsRepository.findByProvider(provider);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/metrics/timerange")
    public ResponseEntity<List<RequestMetrics>> getMetricsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        log.info("Fetching metrics between {} and {}", start, end);
        List<RequestMetrics> metrics = metricsRepository.findByTimestampBetween(start, end);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        log.info("Generating dashboard summary");
        
        Map<String, Object> summary = new HashMap<>();
        
        Long totalRequests = metricsRepository.count();
        Long cacheHits = metricsRepository.getCacheHitCount();
        Long cacheMisses = metricsRepository.getCacheMissCount();
        Double cacheHitRate = totalRequests > 0 ? (cacheHits.doubleValue() / totalRequests) * 100 : 0.0;
        
        Long totalTokensSaved = metricsRepository.getTotalTokensSaved();
        Double avgCompressionPercent = metricsRepository.getAverageCompressionPercent();
        Double avgComplexityScore = metricsRepository.getAverageComplexityScore();
        Double avgUserRating = metricsRepository.getAverageUserRating();
        
        summary.put("totalRequests", totalRequests);
        summary.put("cacheHits", cacheHits);
        summary.put("cacheMisses", cacheMisses);
        summary.put("cacheHitRate", String.format("%.2f%%", cacheHitRate));
        summary.put("totalTokensSaved", totalTokensSaved != null ? totalTokensSaved : 0L);
        summary.put("avgCompressionPercent", avgCompressionPercent != null ? String.format("%.2f%%", avgCompressionPercent) : "N/A");
        summary.put("avgComplexityScore", avgComplexityScore != null ? String.format("%.2f", avgComplexityScore) : "N/A");
        summary.put("avgUserRating", avgUserRating != null ? String.format("%.2f", avgUserRating) : "N/A");
        
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/dashboard/provider-stats")
    public ResponseEntity<Map<String, Object>> getProviderStats() {
        log.info("Generating provider statistics");
        
        Map<String, Object> stats = new HashMap<>();
        
        List<Object[]> requestCounts = metricsRepository.getRequestCountByProvider();
        Map<String, Long> providerRequestCounts = new HashMap<>();
        for (Object[] row : requestCounts) {
            providerRequestCounts.put((String) row[0], (Long) row[1]);
        }
        
        stats.put("requestCountByProvider", providerRequestCounts);
        
        Map<String, Double> avgLatencyByProvider = new HashMap<>();
        for (String provider : providerRequestCounts.keySet()) {
            Double avgLatency = metricsRepository.getAverageLatencyByProvider(provider);
            avgLatencyByProvider.put(provider, avgLatency != null ? avgLatency : 0.0);
        }
        stats.put("avgLatencyByProvider", avgLatencyByProvider);
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/dashboard/performance")
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        log.info("Generating performance metrics");
        
        Map<String, Object> performance = new HashMap<>();
        
        List<RequestMetrics> allMetrics = metricsRepository.findAll();
        
        if (!allMetrics.isEmpty()) {
            double avgLatency = allMetrics.stream()
                    .mapToLong(RequestMetrics::getLatencyMs)
                    .average()
                    .orElse(0.0);
            
            double avgInputTokens = allMetrics.stream()
                    .mapToLong(RequestMetrics::getInputTokens)
                    .average()
                    .orElse(0.0);
            
            double avgOutputTokens = allMetrics.stream()
                    .mapToLong(RequestMetrics::getOutputTokens)
                    .average()
                    .orElse(0.0);
            
            double avgEmbeddingMs = allMetrics.stream()
                    .filter(m -> m.getEmbeddingMs() != null)
                    .mapToLong(RequestMetrics::getEmbeddingMs)
                    .average()
                    .orElse(0.0);
            
            double avgRetrievalMs = allMetrics.stream()
                    .filter(m -> m.getRetrievalMs() != null)
                    .mapToLong(RequestMetrics::getRetrievalMs)
                    .average()
                    .orElse(0.0);
            
            performance.put("avgLatencyMs", String.format("%.2f", avgLatency));
            performance.put("avgInputTokens", String.format("%.2f", avgInputTokens));
            performance.put("avgOutputTokens", String.format("%.2f", avgOutputTokens));
            performance.put("avgEmbeddingMs", String.format("%.2f", avgEmbeddingMs));
            performance.put("avgRetrievalMs", String.format("%.2f", avgRetrievalMs));
        }
        
        return ResponseEntity.ok(performance);
    }

    @PostMapping("/metrics/{requestId}/rating")
    public ResponseEntity<Void> updateUserRating(
            @PathVariable String requestId,
            @RequestParam Integer rating) {
        log.info("Updating user rating for requestId: {} to {}", requestId, rating);
        
        if (rating < 1 || rating > 5) {
            return ResponseEntity.badRequest().build();
        }
        
        observabilityService.updateUserRating(requestId, rating);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/dashboard/cost-analysis")
    public ResponseEntity<Map<String, Object>> getCostAnalysis() {
        log.info("Generating cost analysis");
        
        Map<String, Object> costAnalysis = new HashMap<>();
        
        List<RequestMetrics> allMetrics = metricsRepository.findAll();
        
        double totalEstimatedCost = allMetrics.stream()
                .filter(m -> m.getEstimatedCost() != null)
                .mapToDouble(RequestMetrics::getEstimatedCost)
                .sum();
        
        Long totalTokensSaved = metricsRepository.getTotalTokensSaved();
        
        costAnalysis.put("totalEstimatedCost", String.format("$%.4f", totalEstimatedCost));
        costAnalysis.put("totalTokensSaved", totalTokensSaved != null ? totalTokensSaved : 0L);
        
        return ResponseEntity.ok(costAnalysis);
    }

    @GetMapping("/dashboard/optimization-impact")
    public ResponseEntity<Map<String, Object>> getOptimizationImpact() {
        log.info("Generating optimization impact metrics");
        
        Map<String, Object> impact = new HashMap<>();
        
        List<RequestMetrics> allMetrics = metricsRepository.findAll();
        
        long optimizedCount = allMetrics.stream()
                .filter(m -> m.getWasOptimized() != null && m.getWasOptimized())
                .count();
        
        long totalCount = allMetrics.size();
        double optimizationRate = totalCount > 0 ? (optimizedCount * 100.0 / totalCount) : 0.0;
        
        Double avgSavingsPercent = allMetrics.stream()
                .filter(m -> m.getSavingsPercentage() != null)
                .mapToDouble(RequestMetrics::getSavingsPercentage)
                .average()
                .orElse(0.0);
        
        impact.put("totalRequests", totalCount);
        impact.put("optimizedRequests", optimizedCount);
        impact.put("optimizationRate", String.format("%.2f%%", optimizationRate));
        impact.put("avgSavingsPercent", String.format("%.2f%%", avgSavingsPercent));
        
        return ResponseEntity.ok(impact);
    }
}
