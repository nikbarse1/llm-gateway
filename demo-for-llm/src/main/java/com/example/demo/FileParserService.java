package com.example.demo;

import com.example.demo.chunking.DocumentChunk;
import com.example.demo.chunking.IntelligentChunkingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileParserService {

    private final IntelligentChunkingService chunkingService;

    public FileParserService(IntelligentChunkingService chunkingService) {
        this.chunkingService = chunkingService;
    }

    public String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IOException("File name is null");
        }

        log.info("Parsing uploaded file: {}", filename);

        Resource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        String extension = getFileExtension(filename).toLowerCase();
        List<Document> documents;

        if ("pdf".equals(extension)) {
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
            documents = pdfReader.get();
        } else {
            TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
            documents = tikaReader.get();
        }

        String text = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        log.info("Extracted {} characters from {} ({} sections)", text.length(), filename, documents.size());
        return text;
    }

    public List<DocumentChunk> extractAndChunk(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IOException("File name is null");
        }

        log.info("Parsing and chunking uploaded file: {}", filename);

        Resource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        String extension = getFileExtension(filename).toLowerCase();
        List<Document> documents;

        if ("pdf".equals(extension)) {
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
            documents = pdfReader.get();
        } else {
            TikaDocumentReader tikaReader = new TikaDocumentReader(resource);
            documents = tikaReader.get();
        }

        String text = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        List<DocumentChunk> chunks = chunkingService.chunkDocument(text, filename);
        log.info("Extracted and chunked {} into {} intelligent chunks", filename, chunks.size());
        return chunks;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }
}