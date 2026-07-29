package com.example.demo.llmrouter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PrimaryLlmService {

    private final WebClient webClient;

    @Value("${gemini.api.key}")
    private String apiKey;

    private static final String MODEL = "gemini-2.5-flash";

    public Mono<String> askAi(String optimizedPrompt) {

        log.info("Sending optimized prompt to Gemini");

        Map<String, Object> request =
                Map.of(
                        "contents",
                        List.of(
                                Map.of(
                                        "parts",
                                        List.of(
                                                Map.of("text", optimizedPrompt)
                                        )
                                )
                        )
                );

        return webClient.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/"
                        + MODEL
                        + ":generateContent?key="
                        + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .map(response ->
                        response.candidates()
                                .getFirst()
                                .content()
                                .parts()
                                .getFirst()
                                .text()
                )
                .doOnError(e -> log.error("Gemini API call failed: {}", e.getMessage()));
    }
}