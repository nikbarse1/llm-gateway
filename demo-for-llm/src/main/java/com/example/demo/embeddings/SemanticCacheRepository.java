package com.example.demo.embeddings;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.search.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Repository
@Slf4j
public class SemanticCacheRepository {

    private final JedisPooled jedis;
    private final double similarityThreshold;

    private static final String INDEX_NAME = "idx:semantic_cache";
    private static final String PREFIX = "semantic_cache:";

    public SemanticCacheRepository(
            @Value("${spring.data.redis.host:localhost}") String redisHost,
            @Value("${spring.data.redis.port:6379}") int redisPort,
            @Value("${semantic.cache.similarity.threshold:0.95}") double similarityThreshold) {

        this.jedis = new JedisPooled(redisHost, redisPort);
        this.similarityThreshold = similarityThreshold;
    }

    @PostConstruct
    public void initIndex() {
        try {
            Schema schema = new Schema();
            schema.addTextField("instruction", 1.0);
            schema.addTextField("response", 1.0);
            // New field to guarantee context isolation between identical instructions
            schema.addTagField("contextHash");

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
                    .setPrefixes(new String[]{PREFIX});

            jedis.ftCreate(
                    INDEX_NAME,
                    IndexOptions.defaultOptions().setDefinition(definition),
                    schema
            );
            log.info("Created RediSearch index {}", INDEX_NAME);
        } catch (JedisDataException e) {
            if (e.getMessage().contains("Index already exists")) {
                log.info("Semantic Cache index already exists.");
            } else {
                throw e;
            }
        }
    }

    public void cacheResponse(String instruction, String response, List<Double> embedding, String rawContext) {
        String key = PREFIX + UUID.randomUUID();
        String contextHash = generateHash(rawContext);

        log.info("Caching response under contextHash={}", contextHash);
        jedis.hset(key.getBytes(), Map.of(
                "instruction".getBytes(), instruction.getBytes(),
                "response".getBytes(), response.getBytes(),
                "contextHash".getBytes(), contextHash.getBytes(),
                "embedding".getBytes(), floatArrayToByteArray(embedding)
        ));
    }

    public String findCachedResponse(List<Double> embedding, String rawContext) {
        double maxDistance = 1 - similarityThreshold;
        String contextHash = generateHash(rawContext);

        // Pre-filter by contextHash before executing the vector KNN search
        String queryStr = String.format("(@contextHash:{%s})=>[KNN 1 @embedding $vec AS distance]", contextHash);

        Query query = new Query(queryStr)
                .addParam("vec", floatArrayToByteArray(embedding))
                .returnFields("response", "distance")
                .setSortBy("distance", true)
                .dialect(2);

        SearchResult result = jedis.ftSearch(INDEX_NAME, query);

        if (result.getTotalResults() == 0) {
            log.info("Semantic Cache MISS. No match for context hash.");
            return null;
        }

        Document doc = result.getDocuments().get(0);
        double distance = Double.parseDouble(doc.getString("distance"));

        if (distance <= maxDistance) {
            log.info("Semantic Cache HIT. Distance={}", distance);
            return doc.getString("response");
        }

        log.info("Semantic Cache MISS. Distance {} exceeded threshold.", distance);
        return null;
    }

    /**
     * Generates a fast SHA-256 hash of the context to isolate cache hits.
     */
    public String generateHash(String text) {
        if (text == null || text.isBlank()) {
            return "NONE";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Hashing failed, falling back to string hash code", e);
            return String.valueOf(text.hashCode());
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
}