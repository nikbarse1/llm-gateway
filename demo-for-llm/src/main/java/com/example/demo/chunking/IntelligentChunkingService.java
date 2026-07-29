package com.example.demo.chunking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class IntelligentChunkingService {

    private final List<ChunkingStrategy> strategies;

    public IntelligentChunkingService(
            CodeChunker codeChunker,
            MarkdownChunker markdownChunker,
            PdfChunker pdfChunker,
            TableChunker tableChunker,
            JsonChunker jsonChunker
    ) {
        this.strategies = List.of(codeChunker, markdownChunker, pdfChunker, tableChunker, jsonChunker);
    }

    public List<DocumentChunk> chunkDocument(String content, String filename) {
        String extension = extractExtension(filename);
        
        ChunkingStrategy strategy = strategies.stream()
                .filter(s -> s.supports(extension))
                .findFirst()
                .orElse(null);

        if (strategy != null) {
            log.info("Using {} for file: {}", strategy.getClass().getSimpleName(), filename);
            return strategy.chunk(content);
        }

        log.info("No specific chunker found for extension '{}', using default paragraph chunking", extension);
        return defaultChunking(content);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private List<DocumentChunk> defaultChunking(String content) {
        String[] paragraphs = content.split("\n\n+");
        List<DocumentChunk> chunks = new java.util.ArrayList<>();
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        int position = 0;
        int maxSize = 1500;

        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() > maxSize && currentChunk.length() > 0) {
                chunks.add(DocumentChunk.builder()
                        .content(currentChunk.toString().trim())
                        .chunkIndex(chunkIndex++)
                        .startPosition(position)
                        .endPosition(position + currentChunk.length())
                        .chunkType("paragraph")
                        .metadata(java.util.Map.of())
                        .build());
                position += currentChunk.length();
                currentChunk = new StringBuilder();
            }
            currentChunk.append(paragraph).append("\n\n");
        }

        if (currentChunk.length() > 0) {
            chunks.add(DocumentChunk.builder()
                    .content(currentChunk.toString().trim())
                    .chunkIndex(chunkIndex)
                    .startPosition(position)
                    .endPosition(position + currentChunk.length())
                    .chunkType("paragraph")
                    .metadata(java.util.Map.of())
                    .build());
        }

        log.info("Default chunking created {} chunks", chunks.size());
        return chunks;
    }
}
