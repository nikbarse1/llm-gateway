package com.example.demo.advancePlusOne;

import com.example.demo.TokenCounterService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class PromptTemplateRegistry {

    private final TokenCounterService tokenCounterService;

    private final Map<String, PromptTemplate> templates = new ConcurrentHashMap<>();

    @PostConstruct
    void initDefaults() {
        register("default", "Default Assistant",
                "You are a helpful, highly accurate AI assistant. Process the request inside the current active chat stream context.");
        register("java-expert", "Java Expert",
                "You are a senior Java engineer with deep expertise in Spring Boot, concurrency, JVM internals, and enterprise architecture. " +
                "Provide precise, production-ready answers. Always consider edge cases, thread safety, and performance implications. " +
                "Prefer minimal, focused solutions over over-engineered approaches.");
        register("code-reviewer", "Code Reviewer",
                "You are an expert code reviewer. Analyze the provided code for bugs, security vulnerabilities, performance issues, " +
                "and maintainability. Provide specific, actionable feedback with code examples where appropriate. " +
                "Prioritize findings by severity: critical, high, medium, low.");
        register("architect", "System Architect",
                "You are a principal software architect. Design scalable, resilient, and cost-effective systems. " +
                "Consider trade-offs explicitly: latency vs throughput, consistency vs availability, simplicity vs flexibility. " +
                "Provide architecture diagrams in text form, component responsibilities, and integration patterns.");
        register("debugger", "Debug Specialist",
                "You are a debugging specialist. Systematically isolate root causes from symptoms. " +
                "Ask for specific logs, stack traces, or reproduction steps when needed. " +
                "Propose hypotheses ranked by likelihood, with verification steps for each.");

        log.info("PromptTemplateRegistry initialized with {} default templates: {}", templates.size(), templates.keySet());
    }

    public PromptTemplate register(String id, String name, String content) {
        int tokens = tokenCounterService.countTokens(content);
        PromptTemplate template = PromptTemplate.builder()
                .id(id)
                .name(name)
                .content(content)
                .tokenCount(tokens)
                .build();
        templates.put(id, template);
        log.info("Registered prompt template — id: {}, name: {}, tokens: {}", id, name, tokens);
        return template;
    }

    public Optional<PromptTemplate> resolve(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(templates.get(id));
    }

    public PromptTemplate resolveOrDefault(String id) {
        return resolve(id).orElseGet(() -> templates.get("default"));
    }

    public java.util.Collection<PromptTemplate> listAll() {
        return templates.values();
    }

    public boolean remove(String id) {
        if ("default".equals(id)) {
            log.warn("Cannot remove the 'default' prompt template");
            return false;
        }
        return templates.remove(id) != null;
    }
}
