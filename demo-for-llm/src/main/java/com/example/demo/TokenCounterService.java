package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import com.knuddels.jtokkit.api.IntArrayList;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TokenCounterService {

    private final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    // cl100k_base is extremely efficient and standard for GPT/Llama architectures.
    private final Encoding encoding = registry.getEncodingForModel(ModelType.GPT_4);

    /**
     * Standard OpenAI-based token count. Good for baseline routing heuristics.
     */
    public int countTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return encoding.countTokens(text);
    }

    /**
     * Provider-aware token counting. Applies heuristic multipliers to prevent context
     * window crashes for models that do not use the standard OpenAI tokenizer.
     */
    public int countTokens(String text, String providerName) {
        int baseCount = countTokens(text);

        if ("GEMINI".equalsIgnoreCase(providerName)) {
            // Gemini's SentencePiece tokenizer typically generates ~20% more tokens
            // than OpenAI for the same text, especially for source code.
            return (int) Math.ceil(baseCount * 1.20);
        }

        return baseCount;
    }

    /**
     * Splits a massive string into a list of strings, ensuring no single string
     * exceeds the strict maximum token limit.
     *
     * @param text The raw text to split.
     * @param maxTokensPerChunk The absolute token limit per chunk.
     * @return A list of text chunks safe for LLM consumption.
     */
    public List<String> splitTextByTokens(String text, int maxTokensPerChunk) {
        List<String> textChunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return textChunks;
        }

        IntArrayList tokenIds = encoding.encode(text);
        int totalTokens = tokenIds.size();

        if (totalTokens <= maxTokensPerChunk) {
            textChunks.add(text);
            log.debug("Text of {} tokens fits in single chunk (max {} per chunk)", totalTokens, maxTokensPerChunk);
            return textChunks;
        }

        int start = 0;
        while (start < totalTokens) {
            int end = Math.min(start + maxTokensPerChunk, totalTokens);

            // Extract the sub-array of token IDs for this chunk
            IntArrayList chunkTokens = new IntArrayList(end - start);
            for (int i = start; i < end; i++) {
                chunkTokens.add(tokenIds.get(i));
            }

            // Decode the strict token array back into a readable string
            String chunkText = encoding.decode(chunkTokens);
            textChunks.add(chunkText);

            start = end;
        }

        log.info("Split text into {} chunks (total {} tokens, max {} per chunk)", textChunks.size(), totalTokens, maxTokensPerChunk);
        return textChunks;
    }
}