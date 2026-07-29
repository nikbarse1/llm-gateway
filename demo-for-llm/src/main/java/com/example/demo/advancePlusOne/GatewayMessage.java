package com.example.demo.advancePlusOne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayMessage {
    private Role role;
    private String content;
    private int tokenCount;
    private Instant timestamp;

    public enum Role {
        USER, ASSISTANT, SYSTEM
    }
}
