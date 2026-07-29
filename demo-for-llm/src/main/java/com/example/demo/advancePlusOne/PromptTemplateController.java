package com.example.demo.advancePlusOne;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/prompts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Prompt Template Registry", description = "Register and resolve reusable system prompt templates by ID")
public class PromptTemplateController {

    private final PromptTemplateRegistry registry;

    @Operation(summary = "List all registered prompt templates")
    @GetMapping
    public Collection<PromptTemplate> listTemplates() {
        return registry.listAll();
    }

    @Operation(summary = "Resolve a prompt template by ID")
    @GetMapping("/{id}")
    public ResponseEntity<PromptTemplate> getTemplate(@PathVariable String id) {
        return registry.resolve(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Register a new prompt template")
    @PostMapping
    public PromptTemplate registerTemplate(@RequestBody Map<String, String> body) {
        String id = body.get("id");
        String name = body.get("name");
        String content = body.get("content");
        if (id == null || id.isBlank() || content == null || content.isBlank()) {
            throw new IllegalArgumentException("Fields 'id' and 'content' are required");
        }
        if (name == null || name.isBlank()) {
            name = id;
        }
        return registry.register(id, name, content);
    }

    @Operation(summary = "Delete a prompt template by ID (default cannot be deleted)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable String id) {
        if (registry.remove(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}
