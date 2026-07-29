package com.example.demo.observability;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RequestMetricsRepository extends JpaRepository<RequestMetrics, Long> {

    Optional<RequestMetrics> findByRequestId(String requestId);

    List<RequestMetrics> findByChatId(String chatId);

    List<RequestMetrics> findByProvider(String provider);

    List<RequestMetrics> findByCacheHit(Boolean cacheHit);

    List<RequestMetrics> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT AVG(m.latencyMs) FROM RequestMetrics m WHERE m.provider = :provider")
    Double getAverageLatencyByProvider(@Param("provider") String provider);

    @Query("SELECT AVG(m.compressionPercent) FROM RequestMetrics m WHERE m.compressionPercent IS NOT NULL")
    Double getAverageCompressionPercent();

    @Query("SELECT SUM(m.tokensSaved) FROM RequestMetrics m WHERE m.tokensSaved IS NOT NULL")
    Long getTotalTokensSaved();

    @Query("SELECT m.provider, COUNT(m) FROM RequestMetrics m GROUP BY m.provider")
    List<Object[]> getRequestCountByProvider();

    @Query("SELECT COUNT(m) FROM RequestMetrics m WHERE m.cacheHit = true")
    Long getCacheHitCount();

    @Query("SELECT COUNT(m) FROM RequestMetrics m WHERE m.cacheHit = false")
    Long getCacheMissCount();

    @Query("SELECT AVG(m.complexityScore) FROM RequestMetrics m WHERE m.complexityScore IS NOT NULL")
    Double getAverageComplexityScore();

    @Query("SELECT m FROM RequestMetrics m ORDER BY m.timestamp DESC")
    List<RequestMetrics> findAllOrderByTimestampDesc();

    @Query("SELECT m FROM RequestMetrics m WHERE m.userRating IS NOT NULL ORDER BY m.timestamp DESC")
    List<RequestMetrics> findAllWithUserRating();

    @Query("SELECT AVG(m.userRating) FROM RequestMetrics m WHERE m.userRating IS NOT NULL")
    Double getAverageUserRating();
}
