package com.example.demo.chunking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class JsonChunker implements ChunkingStrategy {

    private static final int MAX_CHUNK_SIZE = 2000;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<DocumentChunk> chunk(String content) {
        List<DocumentChunk> chunks = new ArrayList<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(content);
            
            if (rootNode.isArray()) {
                chunks = chunkJsonArray(rootNode);
            } else if (rootNode.isObject()) {
                chunks = chunkJsonObject(rootNode);
            } else {
                chunks.add(createSimpleChunk(content, 0));
            }
            
        } catch (Exception e) {
            log.warn("Failed to parse JSON, falling back to text chunking: {}", e.getMessage());
            chunks = fallbackChunking(content);
        }

        log.info("JSON chunking created {} chunks", chunks.size());
        return chunks;
    }

    private List<DocumentChunk> chunkJsonArray(JsonNode arrayNode) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;
        List<JsonNode> currentBatch = new ArrayList<>();
        int currentSize = 0;

        for (int i = 0; i < arrayNode.size(); i++) {
            JsonNode element = arrayNode.get(i);
            String elementStr = element.toString();
            
            if (currentSize + elementStr.length() > MAX_CHUNK_SIZE && !currentBatch.isEmpty()) {
                chunks.add(createArrayChunk(currentBatch, chunkIndex++, i - currentBatch.size(), i - 1));
                currentBatch.clear();
                currentSize = 0;
            }
            
            currentBatch.add(element);
            currentSize += elementStr.length();
        }

        if (!currentBatch.isEmpty()) {
            chunks.add(createArrayChunk(currentBatch, chunkIndex, arrayNode.size() - currentBatch.size(), arrayNode.size() - 1));
        }

        return chunks;
    }

    private List<DocumentChunk> chunkJsonObject(JsonNode objectNode) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;
        
        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
        Map<String, JsonNode> currentObject = new LinkedHashMap<>();
        int currentSize = 0;

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldStr = "\"" + field.getKey() + "\":" + field.getValue().toString();
            
            if (currentSize + fieldStr.length() > MAX_CHUNK_SIZE && !currentObject.isEmpty()) {
                chunks.add(createObjectChunk(currentObject, chunkIndex++));
                currentObject.clear();
                currentSize = 0;
            }
            
            currentObject.put(field.getKey(), field.getValue());
            currentSize += fieldStr.length();
        }

        if (!currentObject.isEmpty()) {
            chunks.add(createObjectChunk(currentObject, chunkIndex));
        }

        return chunks;
    }

    private DocumentChunk createArrayChunk(List<JsonNode> elements, int index, int startIdx, int endIdx) {
        try {
            String content = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(elements);
            
            return DocumentChunk.builder()
                    .content(content)
                    .chunkIndex(index)
                    .startPosition(startIdx)
                    .endPosition(endIdx)
                    .chunkType("json_array")
                    .metadata(Map.of(
                            "elementCount", elements.size(),
                            "arrayRange", startIdx + "-" + endIdx
                    ))
                    .build();
        } catch (Exception e) {
            log.error("Failed to serialize JSON array chunk", e);
            return createSimpleChunk(elements.toString(), index);
        }
    }

    private DocumentChunk createObjectChunk(Map<String, JsonNode> fields, int index) {
        try {
            String content = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(fields);
            
            return DocumentChunk.builder()
                    .content(content)
                    .chunkIndex(index)
                    .startPosition(0)
                    .endPosition(content.length())
                    .chunkType("json_object")
                    .metadata(Map.of(
                            "fieldCount", fields.size(),
                            "keys", new ArrayList<>(fields.keySet())
                    ))
                    .build();
        } catch (Exception e) {
            log.error("Failed to serialize JSON object chunk", e);
            return createSimpleChunk(fields.toString(), index);
        }
    }

    private DocumentChunk createSimpleChunk(String content, int index) {
        return DocumentChunk.builder()
                .content(content)
                .chunkIndex(index)
                .startPosition(0)
                .endPosition(content.length())
                .chunkType("json")
                .metadata(Map.of())
                .build();
    }

    private List<DocumentChunk> fallbackChunking(String content) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int pos = 0;
        int index = 0;

        while (pos < content.length()) {
            int end = Math.min(pos + MAX_CHUNK_SIZE, content.length());
            chunks.add(DocumentChunk.builder()
                    .content(content.substring(pos, end))
                    .chunkIndex(index++)
                    .startPosition(pos)
                    .endPosition(end)
                    .chunkType("json_text")
                    .metadata(Map.of())
                    .build());
            pos = end;
        }

        return chunks;
    }

    @Override
    public boolean supports(String fileExtension) {
        return "json".equalsIgnoreCase(fileExtension) || "jsonl".equalsIgnoreCase(fileExtension);
    }
}
