package com.example.demo.chunking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {
    private String content;
    private int chunkIndex;
    private int startPosition;
    private int endPosition;
    private String chunkType;
    private Map<String, Object> metadata;
}
