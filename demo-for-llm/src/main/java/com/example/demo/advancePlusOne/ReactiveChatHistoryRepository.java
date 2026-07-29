package com.example.demo.advancePlusOne;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Slf4j
public class ReactiveChatHistoryRepository {

    // Local stateful store running completely inside your local memory JVM
    private final Map<String, ChatSessionState> storageBudget = new ConcurrentHashMap<>();

    /**
     * Retrieves an existing session state or initializes a empty timeline if missing.
     */
    public Mono<ChatSessionState> findByChatId(String chatId) {
        return Mono.fromSupplier(() -> storageBudget.computeIfAbsent(chatId, id -> {
            log.info("Initializing fresh, stateful database ledger memory for ChatId: {}", id);
            return ChatSessionState.builder()
                    .chatId(id)
                    .build();
        }));
    }

    /**
     * Persists the mutated conversational state matrix back to the local database cache.
     */
    public Mono<ChatSessionState> save(ChatSessionState state) {
        return Mono.fromSupplier(() -> {
            storageBudget.put(state.getChatId(), state);
            log.debug("State saved successfully for ChatId: {}. Active Message Pool Size: {}",
                    state.getChatId(), state.getMessages().size());
            return state;
        });
    }

    /**
     * Flushes history from local runtime cache memory entirely (Useful for testing).
     */
    public Mono<Void> clearSession(String chatId) {
        return Mono.fromRunnable(() -> {
            storageBudget.remove(chatId);
            log.info("Flushed memory map caches clean for session ChatId: {}", chatId);
        });
    }
}
