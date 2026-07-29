package com.example.demo.advancePlusOne;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
public class ChatTitleGeneratorService {

    private final ChatClient chatClient;

    public ChatTitleGeneratorService(
            @Qualifier("openAiChatModel") ChatModel fastTierChatModel) {
        this.chatClient = ChatClient.create(fastTierChatModel);
    }

    public Mono<String> generateTitle(String firstInstruction) {
        log.info("Generating chat title from prompt ({} chars)", firstInstruction.length());

        String prompt = "Summarize the following user prompt into a short, concise chat title (maximum 4 words). Do not use quotes, punctuation, or conversational filler.\n\nPrompt: " + firstInstruction;

        return Mono.fromCallable(() -> chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content())
                .map(String::trim)
                .doOnNext(title -> log.info("Generated chat title: '{}'", title))
                .doOnError(e -> log.warn("Chat title generation failed, using fallback. Error: {}", e.getMessage()))
                .onErrorReturn("New Chat") // Fallback if the API fails
                .subscribeOn(Schedulers.boundedElastic());
    }
}