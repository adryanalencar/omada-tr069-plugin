package com.openomada.plugin.tr069.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-performance sliding-window rate limiter to protect TR-069 ACS endpoints
 * from flood attacks and runaway CPE retries.
 */
@Component
public class RateLimiter {

    private static class Window {
        final long windowStart;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long windowStart) {
            this.windowStart = windowStart;
        }
    }

    private final Map<String, Window> ipWindows = new ConcurrentHashMap<>();
    private int maxRequestsPerWindow = 60; // 60 requests
    private long windowDurationMs = 60_000; // per minute

    public boolean tryAcquire(String ip) {
        if (ip == null || ip.isEmpty()) return true;

        long now = System.currentTimeMillis();
        Window window = ipWindows.compute(ip, (k, existing) -> {
            if (existing == null || (now - existing.windowStart) > windowDurationMs) {
                return new Window(now);
            }
            return existing;
        });

        return window.count.incrementAndGet() <= maxRequestsPerWindow;
    }

    public void setLimits(int maxRequests, long durationMs) {
        this.maxRequestsPerWindow = maxRequests;
        this.windowDurationMs = durationMs;
    }
}
