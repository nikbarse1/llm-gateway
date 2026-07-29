package com.example.demo.chunking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
@Slf4j
public class PdfChunker implements ChunkingStrategy {

    private static final int MAX_CHUNK_SIZE = 1800;
    private static final int OVERLAP_SIZE = 150;
    private static final Pattern PAGE_BREAK_PATTERN = Pattern.compile("\\f");
    private static final Pattern SECTION_PATTERN = Pattern.compile("^\\d+\\.\\s+[A-Z].*$|^[A-Z][A-Z\\s]{3,}$", Pattern.MULTILINE);

    @Override
    public List<DocumentChunk> chunk(String content) {
        List<DocumentChunk> chunks = new ArrayList<>();
        
        String[] pages = PAGE_BREAK_PATTERN.split(content);
        
        int chunkIndex = 0;
        int globalPos = 0;

        for (int pageNum = 0; pageNum < pages.length; pageNum++) {
            String page = pages[pageNum].trim();
            if (page.isEmpty()) continue;

            if (page.length() <= MAX_CHUNK_SIZE) {
                chunks.add(DocumentChunk.builder()
                        .content(page)
                        .chunkIndex(chunkIndex++)
                        .startPosition(globalPos)
                        .endPosition(globalPos + page.length())
                        .chunkType("pdf_page")
                        .metadata(Map.of("pageNumber", pageNum + 1))
                        .build());
            } else {
                List<DocumentChunk> pageChunks = chunkByParagraphs(page, chunkIndex, globalPos, pageNum + 1);
                chunks.addAll(pageChunks);
                chunkIndex += pageChunks.size();
            }
            
            globalPos += page.length();
        }

        log.info("PDF chunking created {} chunks from {} pages", chunks.size(), pages.length);
        return chunks;
    }

    private List<DocumentChunk> chunkByParagraphs(String pageContent, int startIndex, int startPos, int pageNum) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] paragraphs = pageContent.split("\n\n+");
        
        StringBuilder currentChunk = new StringBuilder();
        int index = startIndex;
        int currentPos = startPos;

        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() > MAX_CHUNK_SIZE && currentChunk.length() > 0) {
                chunks.add(DocumentChunk.builder()
                        .content(currentChunk.toString().trim())
                        .chunkIndex(index++)
                        .startPosition(currentPos)
                        .endPosition(currentPos + currentChunk.length())
                        .chunkType("pdf_section")
                        .metadata(Map.of("pageNumber", pageNum, "isSplit", true))
                        .build());
                
                String overlap = getOverlap(currentChunk.toString(), OVERLAP_SIZE);
                currentPos += currentChunk.length() - overlap.length();
                currentChunk = new StringBuilder(overlap);
            }
            currentChunk.append(paragraph).append("\n\n");
        }

        if (currentChunk.length() > 0) {
            chunks.add(DocumentChunk.builder()
                    .content(currentChunk.toString().trim())
                    .chunkIndex(index)
                    .startPosition(currentPos)
                    .endPosition(startPos + pageContent.length())
                    .chunkType("pdf_section")
                    .metadata(Map.of("pageNumber", pageNum))
                    .build());
        }

        return chunks;
    }

    private String getOverlap(String text, int overlapSize) {
        if (text.length() <= overlapSize) return text;
        return text.substring(text.length() - overlapSize);
    }

    @Override
    public boolean supports(String fileExtension) {
        return "pdf".equalsIgnoreCase(fileExtension);
    }
}
