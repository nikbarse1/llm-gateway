package com.example.demo.advancePlusOne;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ChatSessionState {
    private String chatId;

    // Tier 1 & 2: Main historical transcript log
    @Builder.Default
    private List<GatewayMessage> messages = new ArrayList<>();

    // The evolving historical milestone block compressed by Groq
    @Builder.Default
    private String compressedDistantHistory = "";

    public void addMessage(GatewayMessage message) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(message);
    }
}
