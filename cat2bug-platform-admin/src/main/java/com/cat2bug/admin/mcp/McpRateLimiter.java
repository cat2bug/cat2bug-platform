package com.cat2bug.admin.mcp;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按用户的滑动窗口限流（单实例内存计数）。
 */
@ApplicationScoped
public class McpRateLimiter {

    static final long WINDOW_MILLIS = 60_000L;

    @ConfigProperty(name = "cat2bug.mcp.rate-limit.per-minute", defaultValue = "120")
    int perMinute;

    private final Map<Long, Deque<Long>> windows = new ConcurrentHashMap<>();

    /** @return 允许本次调用时为 {@code true} */
    public boolean tryAcquire(Long userId) {
        return tryAcquire(userId, System.currentTimeMillis());
    }

    boolean tryAcquire(Long userId, long now) {
        if (perMinute <= 0) {
            return true;
        }
        Deque<Long> window = windows.computeIfAbsent(userId, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() >= WINDOW_MILLIS) {
                window.pollFirst();
            }
            if (window.size() >= perMinute) {
                return false;
            }
            window.addLast(now);
            return true;
        }
    }

    public int perMinute() {
        return perMinute;
    }

    void reset() {
        windows.clear();
    }
}
