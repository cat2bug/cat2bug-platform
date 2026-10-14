package com.cat2bug.system.service.im;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * 钉钉 / 飞书 / 企业微信访问 token 的进程内缓存。
 * <p>
 * 三家平台都允许同一应用在多个实例上各自持有 token，所以不放进无 TTL 的 {@code AppCacheStore}。
 */
@ApplicationScoped
public class ImTokenCache {

    static final long REFRESH_MARGIN_MILLIS = 5 * 60 * 1000L;
    static final long DEFAULT_EXPIRES_IN_SECONDS = 7000L;

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Map<String, Object> locks = new ConcurrentHashMap<>();
    private volatile LongSupplier clock = System::currentTimeMillis;

    @FunctionalInterface
    public interface TokenLoader {
        ImToken load() throws Exception;
    }

    public record ImToken(String token, long expiresInSeconds) {
    }

    private record Entry(String token, long expiresAt) {
    }

    public String get(String platform, String appId, TokenLoader loader) throws Exception {
        String key = key(platform, appId);
        String cached = validToken(key);
        if (cached != null) {
            return cached;
        }
        synchronized (locks.computeIfAbsent(key, k -> new Object())) {
            cached = validToken(key);
            if (cached != null) {
                return cached;
            }
            ImToken loaded = loader.load();
            long expiresIn = loaded.expiresInSeconds() > 0 ? loaded.expiresInSeconds() : DEFAULT_EXPIRES_IN_SECONDS;
            entries.put(key, new Entry(loaded.token(), clock.getAsLong() + expiresIn * 1000L));
            return loaded.token();
        }
    }

    public void invalidate(String platform, String appId) {
        entries.remove(key(platform, appId));
    }

    void setClock(LongSupplier clock) {
        this.clock = clock;
    }

    private String validToken(String key) {
        Entry entry = entries.get(key);
        if (entry == null || clock.getAsLong() >= entry.expiresAt() - REFRESH_MARGIN_MILLIS) {
            return null;
        }
        return entry.token();
    }

    private static String key(String platform, String appId) {
        return platform + ":" + (appId == null ? "" : appId);
    }
}
