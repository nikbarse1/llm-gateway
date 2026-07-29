package com.example.demo.chunking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class CodeChunker implements ChunkingStrategy {

    private static final int MAX_CHUNK_SIZE = 1500;
    private static final int OVERLAP_SIZE = 200;
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "java", "py", "js", "ts", "jsx", "tsx", "cpp", "c", "h", "cs", "go", "rs", "kt", "swift"
    );

    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
            "(public|private|protected|static|final|abstract|native|synchronized|transient|volatile)?\\s*" +
            "(\\w+\\s+)*\\w+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{",
            Pattern.MULTILINE
    );

    private static final Pattern CLASS_PATTERN = Pattern.compile(
            "(public|private|protected)?\\s*(abstract|final)?\\s*class\\s+\\w+",
            Pattern.MULTILINE
    );

    @Override
    public List<DocumentChunk> chunk(String content) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] lines = content.split("\n");
        
        List<CodeBlock> blocks = extractCodeBlocks(lines);
        
        if (blocks.isEmpty()) {
            return fallbackChunking(content);
        }

        int chunkIndex = 0;
        for (CodeBlock block : blocks) {
            String blockContent = String.join("\n", Arrays.copyOfRange(lines, block.startLine, block.endLine + 1));
            
            if (blockContent.length() > MAX_CHUNK_SIZE) {
                List<DocumentChunk> subChunks = splitLargeBlock(blockContent, chunkIndex, block.startLine);
                chunks.addAll(subChunks);
                chunkIndex += subChunks.size();
            } else {
                chunks.add(DocumentChunk.builder()
                        .content(blockContent)
                        .chunkIndex(chunkIndex++)
                        .startPosition(block.startLine)
                        .endPosition(block.endLine)
                        .chunkType(block.type)
                        .metadata(Map.of("blockType", block.type, "name", block.name))
                        .build());
            }
        }

        log.info("Code chunking created {} chunks", chunks.size());
        return chunks;
    }

    private List<CodeBlock> extractCodeBlocks(String[] lines) {
        List<CodeBlock> blocks = new ArrayList<>();
        Stack<Integer> braceStack = new Stack<>();
        Integer blockStart = null;
        String blockType = "code";
        String blockName = "unnamed";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            Matcher classMatcher = CLASS_PATTERN.matcher(line);
            Matcher functionMatcher = FUNCTION_PATTERN.matcher(line);
            
            if (classMatcher.find() || functionMatcher.find()) {
                if (blockStart != null && !braceStack.isEmpty()) {
                    blocks.add(new CodeBlock(blockStart, i - 1, blockType, blockName));
                }
                blockStart = i;
                blockType = classMatcher.find() ? "class" : "function";
                blockName = extractName(line);
            }

            for (char c : line.toCharArray()) {
                if (c == '{') braceStack.push(i);
                else if (c == '}' && !braceStack.isEmpty()) {
                    braceStack.pop();
                    if (braceStack.isEmpty() && blockStart != null) {
                        blocks.add(new CodeBlock(blockStart, i, blockType, blockName));
                        blockStart = null;
                    }
                }
            }
        }

        if (blockStart != null) {
            blocks.add(new CodeBlock(blockStart, lines.length - 1, blockType, blockName));
        }

        return blocks;
    }

    private String extractName(String line) {
        String[] tokens = line.split("\\s+");
        for (int i = 0; i < tokens.length - 1; i++) {
            if (tokens[i].equals("class") || tokens[i+1].contains("(")) {
                return tokens[i+1].split("\\(")[0];
            }
        }
        return "unnamed";
    }

    private List<DocumentChunk> splitLargeBlock(String content, int startIndex, int startLine) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int pos = 0;
        int index = startIndex;

        while (pos < content.length()) {
            int end = Math.min(pos + MAX_CHUNK_SIZE, content.length());
            String chunk = content.substring(pos, end);
            
            chunks.add(DocumentChunk.builder()
                    .content(chunk)
                    .chunkIndex(index++)
                    .startPosition(startLine + pos)
                    .endPosition(startLine + end)
                    .chunkType("code_fragment")
                    .metadata(Map.of("isSplit", true))
                    .build());
            
            pos = end - OVERLAP_SIZE;
        }

        return chunks;
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
                    .chunkType("code")
                    .metadata(Map.of())
                    .build());
            pos = end - OVERLAP_SIZE;
        }

        return chunks;
    }

    @Override
    public boolean supports(String fileExtension) {
        return SUPPORTED_EXTENSIONS.contains(fileExtension.toLowerCase());
    }

    private static class CodeBlock {
        int startLine;
        int endLine;
        String type;
        String name;

        CodeBlock(int startLine, int endLine, String type, String name) {
            this.startLine = startLine;
            this.endLine = endLine;
            this.type = type;
            this.name = name;
        }
    }
}
