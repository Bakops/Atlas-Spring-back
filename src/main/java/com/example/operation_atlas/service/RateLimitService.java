package com.example.operation_atlas.service;


import com.example.operation_atlas.exception.GameException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static class RateLimitEntry {
        int count;
        Instant windowStart;

        RateLimitEntry() {
            this.count = 1;
            this.windowStart = Instant.now();
        }
    }

    private final Map<String, RateLimitEntry> limits = new ConcurrentHashMap<>();

    public void checkActionLimit(String key, int maxActions, int windowSeconds) {
        Instant now = Instant.now();
        RateLimitEntry entry = limits.computeIfAbsent(key, k -> new RateLimitEntry());

        // Reset si fenêtre expirée
        if (now.isAfter(entry.windowStart.plusSeconds(windowSeconds))) {
            entry.count = 1;
            entry.windowStart = now;
            return;
        }

        entry.count++;
        if (entry.count > maxActions) {
            throw new GameException("ERR_RATE_LIMIT", "Too many requests, please slow down");
        }
    }

    public void checkChatLimit(String playerId) {
        checkActionLimit("chat:" + playerId, 8, 10);
    }

    public void checkGameActionLimit(String ipAddress) {
        checkActionLimit("action:" + ipAddress, 10, 60);
    }
}
