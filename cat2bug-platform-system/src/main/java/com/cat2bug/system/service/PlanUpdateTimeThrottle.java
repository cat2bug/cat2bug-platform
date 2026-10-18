package com.cat2bug.system.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 同一计划在短时间内只刷新一次 {@code update_time}。
 * 并行改多个子项时，每次都更新计划行会让这些事务排队等同一把行锁，
 * 连接一直占到 {@code innodb_lock_wait_timeout}，把连接池耗尽。
 */
public final class PlanUpdateTimeThrottle {

    static final long MIN_INTERVAL_MILLIS = 2_000;

    private final ConcurrentHashMap<String, Long> lastTouched = new ConcurrentHashMap<>();

    public boolean shouldTouch(String planId, long nowMillis) {
        if (planId == null || planId.isBlank()) {
            return false;
        }
        AtomicBoolean allow = new AtomicBoolean(false);
        lastTouched.compute(planId, (key, previous) -> {
            if (previous != null && nowMillis - previous < MIN_INTERVAL_MILLIS) {
                return previous;
            }
            allow.set(true);
            return nowMillis;
        });
        return allow.get();
    }
}
