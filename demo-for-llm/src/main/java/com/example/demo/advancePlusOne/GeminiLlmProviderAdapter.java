package com.example.demo.advancePlusOne;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GeminiLlmProviderAdapter implements LlmProvider {

    private final ChatClient chatClient;

    public GeminiLlmProviderAdapter(
            @Qualifier("googleGenAiChatModel") ChatModel chatModel) {

        this.chatClient = ChatClient.create(chatModel);
    }

    @Override
    public Mono<ChatResponse> askAi(Prompt prompt) {
        log.info("Gateway routing execution to Spring AI Gemini...");
        log.info("Gemini prompt - messages: {}, content preview: '{}'",
                prompt.getInstructions().size(), truncate(prompt.getInstructions().toString(), 500));
        return Mono.fromCallable(() -> {
                    ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
                    String text = response.getResult().getOutput().getText();
                    log.info("Gemini response - length: {}", text != null ? text.length() : 0);
                    return response;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.warn("Gemini call failed: {}", e.getMessage()));
    }

    private static String truncate(String text, int max) {
        if (text == null || text.length() <= max) return text;
        return text.substring(0, max) + "...";
    }

    @Override
    public String getProviderName() {
        return "GEMINI";
    }
}