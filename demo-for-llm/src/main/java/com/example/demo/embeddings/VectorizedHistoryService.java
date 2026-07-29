package com.example.demo.embeddings;

import com.example.demo.embeddings.GeminiEmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VectorizedHistoryService {

    private final GeminiEmbeddingService embeddingService;
    private final JedisPooled jedis;

    private static final String HISTORY_INDEX_NAME = "idx:chat_history";
    private static final String HISTORY_PREFIX = "chat_history:";

    public VectorizedHistoryService(GeminiEmbeddingService embeddingService, JedisPooled jedis) {
        this.embeddingService = embeddingService;
        this.jedis = jedis;
        initHistoryIndex();
    }

    private void initHistoryIndex() {
        try {
            Schema schema = new Schema();
            schema.addTagField("chatId");
            // Numeric field allows us to sort chronologically after semantic retrieval
            schema.addNumericField("timestamp");
            schema.addTextField("role", 1.0);
            schema.addTextField("content", 1.0);
            schema.addVectorField(
                    "embedding",
                    Schema.VectorField.VectorAlgo.HNSW,
                    Map.of(
                            "TYPE", "FLOAT32",
                            "DIM", 768,
                            "DISTANCE_METRIC", "COSINE"
                    )
            );

            IndexDefinition definition = new IndexDefinition(IndexDefinition.Type.HASH)
                    .setPrefixes(new String[]{HISTORY_PREFIX});

            jedis.ftCreate(
                    HISTORY_INDEX_NAME,
                    IndexOptions.defaultOptions().setDefinition(definition),
                    schema
            );
            log.info("Initialized Redis History Index: {}", HISTORY_INDEX_NAME);
        } catch (Exception e) {
            log.debug("History index check/creation notice: {}", e.getMessage());
        }
    }

    /**
     * Saves a conversational turn (User or Assistant) into Redis as a searchable vector.
     */
    public Mono<Void> saveMessageToHistory(String chatId, String role, String content) {
        if (content == null || content.isBlank()) {
            return Mono.empty();
        }
        log.info("Saving history message chatId={}, role={}, contentLength={}", chatId, role, content.length());
        return embeddingService.generateEmbedding(content)
                .flatMap(embedding -> Mono.fromRunnable(() -> {
                    if (embedding != null && !embedding.isEmpty()) {
                        long timestamp = Instant.now().toEpochMilli();
                        String key = HISTORY_PREFIX + chatId + ":" + timestamp;

                        jedis.hset(key.getBytes(), Map.of(
                                "chatId".getBytes(), chatId.getBytes(),
                                "timestamp".getBytes(), String.valueOf(timestamp).getBytes(),
                                "role".getBytes(), role.getBytes(),
                                "content".getBytes(), content.getBytes(),
                                "embedding".getBytes(), floatArrayToByteArray(embedding)
                        ));
                    }
                }).subscribeOn(Schedulers.boundedElastic()))
                .doOnError(e -> log.error("Failed to save history for chatId={}: {}", chatId, e.getMessage()))
                .then();
    }

    /**
     * Finds previous conversation turns that are semantically relevant to the current prompt.
     */
    public Mono<String> retrieveRelevantHistory(String chatId, String currentPrompt, int topK) {
        return embeddingService.generateEmbedding(currentPrompt)
                .flatMap(promptEmbedding -> Mono.fromCallable(() -> {
                    if (promptEmbedding == null || promptEmbedding.isEmpty()) return "";

                    // Filter strictly by the current ChatId, then find nearest semantic neighbors
                    String escapedChatId = escapeTagValue(chatId);
                    String queryStr = String.format("(@chatId:{%s})=>[KNN %d @embedding $vec AS distance]", escapedChatId, topK);                    Query query = new Query(queryStr)
                            .addParam("vec", floatArrayToByteArray(promptEmbedding))
                            .returnFields("role", "content", "timestamp")
                            .setSortBy("timestamp", true) // Ensure the retrieved blocks are chronological
                            .dialect(2);

                    SearchResult result = jedis.ftSearch(HISTORY_INDEX_NAME, query);

                    if (result.getTotalResults() == 0) {
                        log.debug("No relevant history found for chatId={}", chatId);
                        return "";
                    }

                    log.info("Retrieved {} relevant history documents for chatId={}", result.getTotalResults(), chatId);
                    return result.getDocuments().stream()
                            .filter(doc -> doc.hasProperty("content"))
                            .map(doc -> doc.getString("role") + ": " + doc.getString("content"))
                            .collect(Collectors.joining("\n"));

                }).subscribeOn(Schedulers.boundedElastic()))
                .doOnError(e -> log.error("Failed to retrieve relevant history for chatId={}: {}", chatId, e.getMessage()));
    }

    /**
     * Counts the total number of history messages stored for a given chatId.
     */
    public int countMessagesByChatId(String chatId) {
        try {
            String escapedChatId = escapeTagValue(chatId);
            String queryStr = String.format("@chatId:{%s}", escapedChatId);
            Query query = new Query(queryStr).limit(0, 0).dialect(2);
            SearchResult result = jedis.ftSearch(HISTORY_INDEX_NAME, query);
            return (int) result.getTotalResults();
        } catch (Exception e) {
            log.error("Failed to count history for chatId={}: {}", chatId, e.getMessage());
            return 0;
        }
    }

    /**
     * Retrieves ALL history messages for a chatId in chronological order (no semantic filtering).
     */
    public String retrieveAllHistoryByChatId(String chatId) {
        try {
            String escapedChatId = escapeTagValue(chatId);
            String queryStr = String.format("@chatId:{%s}", escapedChatId);
            Query query = new Query(queryStr)
                    .returnFields("role", "content", "timestamp")
                    .setSortBy("timestamp", true)
                    .limit(0, 1000)
                    .dialect(2);

            SearchResult result = jedis.ftSearch(HISTORY_INDEX_NAME, query);
            if (result.getTotalResults() == 0) {
                return "";
            }

            return result.getDocuments().stream()
                    .filter(doc -> doc.hasProperty("content"))
                    .map(doc -> doc.getString("role") + ": " + doc.getString("content"))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.error("Failed to retrieve all history for chatId={}: {}", chatId, e.getMessage());
            return "";
        }
    }

    /**
     * Deletes all history messages for a chatId. Used after summarization replaces old turns.
     */
    public void deleteHistoryByChatId(String chatId) {
        try {
            String escapedChatId = escapeTagValue(chatId);
            String queryStr = String.format("@chatId:{%s}", escapedChatId);
            Query query = new Query(queryStr).returnFields("id").limit(0, 1000).dialect(2);
            SearchResult result = jedis.ftSearch(HISTORY_INDEX_NAME, query);

            for (redis.clients.jedis.search.Document doc : result.getDocuments()) {
                jedis.del(doc.getId());
            }
            log.info("Deleted {} history entries for chatId={}", result.getTotalResults(), chatId);
        } catch (Exception e) {
            log.error("Failed to delete history for chatId={}: {}", chatId, e.getMessage());
        }
    }

    private byte[] floatArrayToByteArray(List<Double> vector) {
        byte[] bytes = new byte[Float.BYTES * vector.size()];
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        for (Double val : vector) {
            buffer.putFloat(val.floatValue());
        }
        return bytes;
    }

    // Add this helper method to your VectorizedHistoryService
    private String escapeTagValue(String value) {
        if (value == null) return null;
        // Escape all hyphens with a double backslash for the query parser
        return value.replace("-", "\\-");
    }
}