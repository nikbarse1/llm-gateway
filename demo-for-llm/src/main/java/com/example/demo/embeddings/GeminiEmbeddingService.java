package com.example.demo.embeddings;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class GeminiEmbeddingService {

    private final EmbeddingModel embeddingModel;

    public GeminiEmbeddingService(@Qualifier("googleGenAiTextEmbedding") EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * Converts a raw text string into a dense mathematical vector using Spring AI.
     */
    public Mono<List<Double>> generateEmbedding(String text) {
        if (text == null || text.isBlank()) {
            return Mono.empty();
        }

        log.debug("Generating embedding for text length={}", text.length());
        return Mono.fromCallable(() -> {
                    // Spring AI returns float[] arrays by default for Embeddings.
                    // We map it to List<Double> to maintain compatibility with your existing Redis Semantic Cache logic.
                    float[] embedArray = embeddingModel.embed(text);
                    List<Double> doubleList = new ArrayList<>(embedArray.length);
                    for (float f : embedArray) {
                        doubleList.add((double) f);
                    }
                    return doubleList;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.error("Failed to generate vector embedding: {}", e.getMessage()));
    }
}