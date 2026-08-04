package com.example.demo;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ModelDebug implements CommandLineRunner {

    private final OpenAiChatModel model;

    public ModelDebug(OpenAiChatModel model) {
        this.model = model;
    }

    @Override
    public void run(String... args) {
        System.out.println(model);
    }

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        System.out.println(apiKey.substring(0, 12));
    }
}
