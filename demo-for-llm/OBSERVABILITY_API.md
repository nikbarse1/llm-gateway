# Observability API Documentation

## Overview
The observability system automatically captures and persists metrics for every LLM request, including:
- Request ID (unique identifier)
- Provider used
- Latency (total request time)
- Input/Output tokens
- Cache hit status
- Embedding generation time
- Retrieval time
- Compression percentage
- Estimated cost
- User rating
- Complexity score

## Database Access
- **H2 Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:file:./data/observability`
  - Username: `sa`
  - Password: (empty)

## REST API Endpoints

### 1. Get All Metrics
```
GET /api/observability/metrics
```
Returns all request metrics ordered by timestamp (descending).

**Response**: Array of `RequestMetrics` objects

---

### 2. Get Metrics by Request ID
```
GET /api/observability/metrics/{requestId}
```
Returns metrics for a specific request.

**Parameters**:
- `requestId` (path): Unique request identifier

**Response**: Single `RequestMetrics` object

---

### 3. Get Metrics by Chat ID
```
GET /api/observability/metrics/chat/{chatId}
```
Returns all metrics for a specific chat session.

**Parameters**:
- `chatId` (path): Chat session identifier

**Response**: Array of `RequestMetrics` objects

---

### 4. Get Metrics by Provider
```
GET /api/observability/metrics/provider/{provider}
```
Returns all metrics for a specific LLM provider.

**Parameters**:
- `provider` (path): Provider name (e.g., "GEMINI", "FAST_TIER", "CACHE")

**Response**: Array of `RequestMetrics` objects

---

### 5. Get Metrics by Time Range
```
GET /api/observability/metrics/timerange?start={start}&end={end}
```
Returns metrics within a specific time range.

**Parameters**:
- `start` (query): Start timestamp (ISO 8601 format)
- `end` (query): End timestamp (ISO 8601 format)

**Example**:
```
GET /api/observability/metrics/timerange?start=2026-07-29T00:00:00&end=2026-07-29T23:59:59
```

**Response**: Array of `RequestMetrics` objects

---

### 6. Dashboard Summary
```
GET /api/observability/dashboard/summary
```
Returns high-level summary statistics.

**Response**:
```json
{
  "totalRequests": 1250,
  "cacheHits": 450,
  "cacheMisses": 800,
  "cacheHitRate": "36.00%",
  "totalTokensSaved": 125000,
  "avgCompressionPercent": "42.50%",
  "avgComplexityScore": "3.75",
  "avgUserRating": "4.20"
}
```

---

### 7. Provider Statistics
```
GET /api/observability/dashboard/provider-stats
```
Returns statistics grouped by provider.

**Response**:
```json
{
  "requestCountByProvider": {
    "GEMINI": 800,
    "FAST_TIER": 350,
    "CACHE": 100
  },
  "avgLatencyByProvider": {
    "GEMINI": 2500.50,
    "FAST_TIER": 1800.25,
    "CACHE": 50.00
  }
}
```

---

### 8. Performance Metrics
```
GET /api/observability/dashboard/performance
```
Returns average performance metrics across all requests.

**Response**:
```json
{
  "avgLatencyMs": "2150.75",
  "avgInputTokens": "1250.50",
  "avgOutputTokens": "850.25",
  "avgEmbeddingMs": "150.00",
  "avgRetrievalMs": "300.50"
}
```

---

### 9. Update User Rating
```
POST /api/observability/metrics/{requestId}/rating?rating={rating}
```
Updates the user rating for a specific request.

**Parameters**:
- `requestId` (path): Unique request identifier
- `rating` (query): Rating value (1-5)

**Response**: 200 OK (no body)

---

### 10. Cost Analysis
```
GET /api/observability/dashboard/cost-analysis
```
Returns cost-related metrics.

**Response**:
```json
{
  "totalEstimatedCost": "$12.5678",
  "totalTokensSaved": 125000
}
```

---

### 11. Optimization Impact
```
GET /api/observability/dashboard/optimization-impact
```
Returns metrics about optimization effectiveness.

**Response**:
```json
{
  "totalRequests": 1250,
  "optimizedRequests": 950,
  "optimizationRate": "76.00%",
  "avgSavingsPercent": "42.50%"
}
```

---

## RequestMetrics Schema

```json
{
  "id": 1,
  "requestId": "uuid-string",
  "timestamp": "2026-07-29T16:30:00",
  "provider": "GEMINI",
  "latencyMs": 2500,
  "inputTokens": 1200,
  "outputTokens": 800,
  "cacheHit": false,
  "embeddingMs": 150,
  "retrievalMs": 300,
  "compressionPercent": 42.5,
  "estimatedCost": 0.0025,
  "userRating": 5,
  "complexityScore": 4.2,
  "chatId": "chat-session-123",
  "sourceType": "FILE",
  "wasOptimized": true,
  "totalTokens": 2000,
  "tokensSaved": 500,
  "savingsPercentage": 20.0,
  "requestedProvider": "GEMINI",
  "executedProvider": "GEMINI",
  "contextWindow": 120000,
  "remainingHeadroom": 118000
}
```

## Usage Examples

### Example 1: Get Recent Metrics
```bash
curl http://localhost:8080/api/observability/metrics
```

### Example 2: Get Dashboard Summary
```bash
curl http://localhost:8080/api/observability/dashboard/summary
```

### Example 3: Update User Rating
```bash
curl -X POST "http://localhost:8080/api/observability/metrics/abc-123/rating?rating=5"
```

### Example 4: Get Provider Performance
```bash
curl http://localhost:8080/api/observability/dashboard/provider-stats
```

### Example 5: Get Metrics for a Time Range
```bash
curl "http://localhost:8080/api/observability/metrics/timerange?start=2026-07-29T00:00:00&end=2026-07-29T23:59:59"
```

## Integration Notes

1. **Automatic Capture**: Metrics are automatically captured for every request through `AdvancedGatewayOrchestrationService`
2. **Async Persistence**: Metrics are persisted asynchronously to avoid blocking the main request flow
3. **Cost Calculation**: Costs are estimated based on provider-specific pricing:
   - GEMINI: $0.075/1M input, $0.30/1M output
   - FAST_TIER: $0.15/1M input, $0.60/1M output
   - CACHE: $0 (free)
4. **Complexity Score**: Calculated by the routing service based on multiple evaluators

## Future Enhancements

Consider building a frontend dashboard using these endpoints to visualize:
- Real-time request metrics
- Cost trends over time
- Cache hit rate charts
- Provider performance comparisons
- User satisfaction ratings
- Token savings analytics
