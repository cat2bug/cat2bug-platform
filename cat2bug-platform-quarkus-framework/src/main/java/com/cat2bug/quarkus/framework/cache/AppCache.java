package com.cat2bug.quarkus.framework.cache;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Collection;

/**
 * 应用级缓存抽象：dev 默认本地内存；生产可切换 Redis（与 J2Cache 对齐的 interim 方案）。
 */
public interface AppCache {

    <T> T get(String region, String key, Class<T> type);

    <T> T get(String region, String key, TypeReference<T> typeRef);

    void set(String region, String key, Object value);

    void remove(String region, String key);

    void clearRegion(String region);

    /** 返回区域内所有键（不含 {@code region:} 前缀，与 Spring J2Cache {@code keys(group)} 对齐）。 */
    Collection<String> keysInRegion(String region);
}
