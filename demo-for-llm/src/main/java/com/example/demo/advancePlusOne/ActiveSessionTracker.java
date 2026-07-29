package com.example.demo.advancePlusOne;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ActiveSessionTracker {

    // Maps ChatId -> Chat Title
    private final Map<String, String> sessionTitles = new ConcurrentHashMap<>();

    public void registerSession(String chatId, String title) {
        if (chatId != null && !chatId.isBlank()) {
            sessionTitles.put(chatId, title);
            log.info("Registered session chatId={}, title='{}'", chatId, title);
        }
    }

    public void updateTitle(String chatId, String newTitle) {
        if (sessionTitles.containsKey(chatId)) {
            sessionTitles.put(chatId, newTitle);
            log.info("Updated title for chatId={} to '{}'", chatId, newTitle);
        }
    }

    public Map<String, String> getAllSessions() {
        log.debug("Retrieved all sessions. Count={}", sessionTitles.size());
        return sessionTitles;
    }

    public void clearSession(String chatId) {
        sessionTitles.remove(chatId);
        log.info("Cleared session chatId={}", chatId);
    }

    public boolean sessionExists(String chatId) {
        boolean exists = sessionTitles.containsKey(chatId);
        log.debug("Checked existence for chatId={}: {}", chatId, exists);
        return exists;
    }
}