package com.example.demo.chunking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class MarkdownChunker implements ChunkingStrategy {

    private static final int MAX_CHUNK_SIZE = 2000;
    private static final int OVERLAP_SIZE = 100;
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```", Pattern.MULTILINE);

    @Override
    public List<DocumentChunk> chunk(String content) {
        List<DocumentChunk> chunks = new ArrayList<>();
        List<MarkdownSection> sections = extractSections(content);

        int chunkIndex = 0;
        for (MarkdownSection section : sections) {
            if (section.content.length() > MAX_CHUNK_SIZE) {
                List<DocumentChunk> subChunks = splitLargeSection(section, chunkIndex);
                chunks.addAll(subChunks);
                chunkIndex += subChunks.size();
            } else {
                chunks.add(DocumentChunk.builder()
                        .content(section.content)
                        .chunkIndex(chunkIndex++)
                        .startPosition(section.startPos)
                        .endPosition(section.endPos)
                        .chunkType("markdown_section")
                        .metadata(Map.of(
                                "heading", section.heading,
                                "level", section.level,
                                "hasCodeBlock", section.hasCodeBlock
                        ))
                        .build());
            }
        }

        log.info("Markdown chunking created {} chunks from {} sections", chunks.size(), sections.size());
        return chunks;
    }

    private List<MarkdownSection> extractSections(String content) {
        List<MarkdownSection> sections = new ArrayList<>();
        Matcher matcher = HEADING_PATTERN.matcher(content);
        
        List<HeadingPosition> headings = new ArrayList<>();
        while (matcher.find()) {
            headings.add(new HeadingPosition(
                    matcher.start(),
                    matcher.group(1).length(),
                    matcher.group(2).trim()
            ));
        }

        if (headings.isEmpty()) {
            sections.add(new MarkdownSection(
                    content, 0, content.length(), "Document", 0, containsCodeBlock(content)
            ));
            return sections;
        }

        for (int i = 0; i < headings.size(); i++) {
            HeadingPosition current = headings.get(i);
            int endPos = (i < headings.size() - 1) ? headings.get(i + 1).position : content.length();
            
            String sectionContent = content.substring(current.position, endPos).trim();
            sections.add(new MarkdownSection(
                    sectionContent,
                    current.position,
                    endPos,
                    current.text,
                    current.level,
                    containsCodeBlock(sectionContent)
            ));
        }

        return sections;
    }

    private boolean containsCodeBlock(String content) {
        return CODE_BLOCK_PATTERN.matcher(content).find();
    }

    private List<DocumentChunk> splitLargeSection(MarkdownSection section, int startIndex) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String[] paragraphs = section.content.split("\n\n+");
        
        StringBuilder currentChunk = new StringBuilder();
        int index = startIndex;
        int currentPos = section.startPos;

        for (String paragraph : paragraphs) {
            if (currentChunk.length() + paragraph.length() > MAX_CHUNK_SIZE && currentChunk.length() > 0) {
                chunks.add(DocumentChunk.builder()
                        .content(currentChunk.toString().trim())
                        .chunkIndex(index++)
                        .startPosition(currentPos)
                        .endPosition(currentPos + currentChunk.length())
                        .chunkType("markdown_fragment")
                        .metadata(Map.of("parentHeading", section.heading))
                        .build());
                
                currentPos += currentChunk.length();
                currentChunk = new StringBuilder();
            }
            currentChunk.append(paragraph).append("\n\n");
        }

        if (currentChunk.length() > 0) {
            chunks.add(DocumentChunk.builder()
                    .content(currentChunk.toString().trim())
                    .chunkIndex(index)
                    .startPosition(currentPos)
                    .endPosition(section.endPos)
                    .chunkType("markdown_fragment")
                    .metadata(Map.of("parentHeading", section.heading))
                    .build());
        }

        return chunks;
    }

    @Override
    public boolean supports(String fileExtension) {
        return "md".equalsIgnoreCase(fileExtension) || "markdown".equalsIgnoreCase(fileExtension);
    }

    private static class MarkdownSection {
        String content;
        int startPos;
        int endPos;
        String heading;
        int level;
        boolean hasCodeBlock;

        MarkdownSection(String content, int startPos, int endPos, String heading, int level, boolean hasCodeBlock) {
            this.content = content;
            this.startPos = startPos;
            this.endPos = endPos;
            this.heading = heading;
            this.level = level;
            this.hasCodeBlock = hasCodeBlock;
        }
    }

    private static class HeadingPosition {
        int position;
        int level;
        String text;

        HeadingPosition(int position, int level, String text) {
            this.position = position;
            this.level = level;
            this.text = text;
        }
    }
}
