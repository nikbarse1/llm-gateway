package com.example.demo.chunking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class TableChunker implements ChunkingStrategy {

    private static final int MAX_ROWS_PER_CHUNK = 50;
    private static final Pattern CSV_LINE_PATTERN = Pattern.compile("(?:,|\\n|^)(\"(?:(?:\"\")*[^\"]*)*\"|[^\",\\n]*)", Pattern.MULTILINE);
    private static final Pattern MARKDOWN_TABLE_PATTERN = Pattern.compile("^\\|.+\\|$", Pattern.MULTILINE);

    @Override
    public List<DocumentChunk> chunk(String content) {
        List<DocumentChunk> chunks = new ArrayList<>();
        
        if (isMarkdownTable(content)) {
            chunks = chunkMarkdownTable(content);
        } else {
            chunks = chunkCsvTable(content);
        }

        log.info("Table chunking created {} chunks", chunks.size());
        return chunks;
    }

    private boolean isMarkdownTable(String content) {
        return MARKDOWN_TABLE_PATTERN.matcher(content).find();
    }

    private List<DocumentChunk> chunkMarkdownTable(String content) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] lines = content.split("\n");
        
        List<String> headers = new ArrayList<>();
        List<List<String>> currentTable = new ArrayList<>();
        int tableStart = 0;
        int chunkIndex = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            if (line.startsWith("|") && line.endsWith("|")) {
                if (headers.isEmpty()) {
                    headers.add(line);
                    tableStart = i;
                } else if (line.matches("^\\|[\\s:-]+\\|$")) {
                    headers.add(line);
                } else {
                    currentTable.add(Arrays.asList(line.split("\\|")));
                    
                    if (currentTable.size() >= MAX_ROWS_PER_CHUNK) {
                        chunks.add(createTableChunk(headers, currentTable, chunkIndex++, tableStart, i));
                        currentTable.clear();
                        tableStart = i + 1;
                    }
                }
            } else if (!currentTable.isEmpty()) {
                chunks.add(createTableChunk(headers, currentTable, chunkIndex++, tableStart, i - 1));
                currentTable.clear();
                headers.clear();
            }
        }

        if (!currentTable.isEmpty()) {
            chunks.add(createTableChunk(headers, currentTable, chunkIndex, tableStart, lines.length - 1));
        }

        return chunks;
    }

    private List<DocumentChunk> chunkCsvTable(String content) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] lines = content.split("\n");
        
        if (lines.length == 0) return chunks;

        String header = lines[0];
        List<String> rows = new ArrayList<>();
        int chunkIndex = 0;
        int startLine = 1;

        for (int i = 1; i < lines.length; i++) {
            rows.add(lines[i]);
            
            if (rows.size() >= MAX_ROWS_PER_CHUNK) {
                chunks.add(createCsvChunk(header, rows, chunkIndex++, startLine, i));
                rows.clear();
                startLine = i + 1;
            }
        }

        if (!rows.isEmpty()) {
            chunks.add(createCsvChunk(header, rows, chunkIndex, startLine, lines.length - 1));
        }

        return chunks;
    }

    private DocumentChunk createTableChunk(List<String> headers, List<List<String>> rows, int index, int start, int end) {
        StringBuilder content = new StringBuilder();
        headers.forEach(h -> content.append(h).append("\n"));
        rows.forEach(row -> content.append(String.join("|", row)).append("\n"));

        return DocumentChunk.builder()
                .content(content.toString().trim())
                .chunkIndex(index)
                .startPosition(start)
                .endPosition(end)
                .chunkType("markdown_table")
                .metadata(Map.of(
                        "rowCount", rows.size(),
                        "columnCount", headers.isEmpty() ? 0 : headers.get(0).split("\\|").length - 2
                ))
                .build();
    }

    private DocumentChunk createCsvChunk(String header, List<String> rows, int index, int start, int end) {
        StringBuilder content = new StringBuilder(header).append("\n");
        rows.forEach(row -> content.append(row).append("\n"));

        int columnCount = header.split(",").length;

        return DocumentChunk.builder()
                .content(content.toString().trim())
                .chunkIndex(index)
                .startPosition(start)
                .endPosition(end)
                .chunkType("csv_table")
                .metadata(Map.of(
                        "rowCount", rows.size(),
                        "columnCount", columnCount,
                        "hasHeader", true
                ))
                .build();
    }

    @Override
    public boolean supports(String fileExtension) {
        return "csv".equalsIgnoreCase(fileExtension) || 
               "tsv".equalsIgnoreCase(fileExtension) ||
               "table".equalsIgnoreCase(fileExtension);
    }
}
