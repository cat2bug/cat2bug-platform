package com.cat2bug.framework.web;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * 进程内按「桶 + IP」的固定一分钟窗口限流。单实例部署足够；多实例时各实例独立计数。
 */
@ApplicationScoped
public class IpRateLimiter {

    private static final long WINDOW_MILLIS = 60_000L;
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public IpRateLimiter() {
        this(System::currentTimeMillis);
    }

    public IpRateLimiter(LongSupplier clock) {
        this.clock = clock;
    }

    /**
     * 限流用的客户端地址。直连来源是回环或内网地址（反向代理）时才信任 {@code X-Forwarded-For}，
     * 并取最后一段（离代理最近、由代理追加的那一段）；公网直连时忽略该头，防止伪造绕过限流。
     */
    public static String clientKey(HttpServerRequest request) {
        SocketAddress remote = request.remoteAddress();
        String remoteHost = remote == null ? null : remote.hostAddress();
        if (remoteHost != null && !isProxyAddress(remoteHost)) {
            return remoteHost;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            String last = parts[parts.length - 1].trim();
            if (!last.isEmpty()) {
                return last;
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return remoteHost == null ? "unknown" : remoteHost;
    }

    public static boolean isProxyAddress(String host) {
        if (host == null || !host.matches("[0-9a-fA-F:.%a-zA-Z]+") || !(host.contains(":") || host.matches("[0-9.]+"))) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isLoopbackAddress() || address.isSiteLocalAddress() || address.isLinkLocalAddress();
        } catch (Exception e) {
            return false;
        }
    }

    /** 返回 false 表示本分钟内已超过 {@code limitPerMinute} */
    public boolean tryAcquire(String bucket, String ip, int limitPerMinute) {
        if (limitPerMinute <= 0) {
            return true;
        }
        long now = clock.getAsLong();
        long windowStart = now - (now % WINDOW_MILLIS);
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(w -> w.start < windowStart);
        }
        Window window = windows.compute(bucket + "|" + ip, (key, current) ->
                current == null || current.start != windowStart ? new Window(windowStart) : current);
        return window.count.incrementAndGet() <= limitPerMinute;
    }

    private static final class Window {
        final long start;
        final AtomicInteger count = new AtomicInteger();

        Window(long start) {
            this.start = start;
        }
    }
}
