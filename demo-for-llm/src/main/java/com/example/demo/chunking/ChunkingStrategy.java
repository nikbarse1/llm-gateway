package com.example.demo.chunking;

import java.util.List;

public interface ChunkingStrategy {
    List<DocumentChunk> chunk(String content);
    boolean supports(String fileExtension);
}
