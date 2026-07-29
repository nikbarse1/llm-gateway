package com.example.demo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

@Configuration
public class LLMConfig {

    @Bean
    public ChatMemory chatMemory() {
        return new ChatMemory() {
            private final Map<String, List<Message>> memory = new ConcurrentHashMap<>();

            @Override
            public void add(String conversationId, List<Message> messages) {
                memory.computeIfAbsent(conversationId, k -> new ArrayList<>()).addAll(messages);
            }

            @Override
            public List<Message> get(String conversationId) {
                return memory.getOrDefault(conversationId, new ArrayList<>());
            }

            @Override
            public void clear(String conversationId) {
                memory.remove(conversationId);
            }
        };
    }

//    @Bean
//    public ChatClient.Builder chatClientBuilder(ChatClient.Builder builder) {
//        return builder;
//    }
}