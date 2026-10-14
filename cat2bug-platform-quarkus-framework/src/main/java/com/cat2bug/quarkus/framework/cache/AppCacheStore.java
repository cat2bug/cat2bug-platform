package com.cat2bug.quarkus.framework.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用缓存：local 内存或 Redis（键格式 {@code region:key}，与 J2Cache generic 区域对齐）。
 */
@ApplicationScoped
public class AppCacheStore implements AppCache {

    private static final Logger LOG = Logger.getLogger(AppCacheStore.class);

    @ConfigProperty(name = "cat2bug.cache.type", defaultValue = "local")
    String cacheType;

    @ConfigProperty(name = "cat2bug.redis.hosts")
    java.util.Optional<String> redisHosts;

    @Inject
    ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Object> memory = new ConcurrentHashMap<>();
    private boolean useRedis;
    private JedisPool jedisPool;

    @PostConstruct
    void init() {
        boolean redisConfigured = redisHosts.filter(s -> !s.isBlank()).isPresent();
        if ("redis".equalsIgnoreCase(cacheType)) {
            useRedis = redisConfigured;
            if (!useRedis) {
                throw new IllegalStateException("cat2bug.cache.type=redis 但未配置 cat2bug.redis.hosts");
            }
        } else {
            useRedis = false;
        }
        if (useRedis) {
            jedisPool = createPool(redisHosts.orElseThrow());
        }
        LOG.infof("应用缓存: %s", useRedis ? "redis" : "local");
    }

    @PreDestroy
    void shutdown() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String region, String key, Class<T> type) {
        if (useRedis) {
            return getRedis(region, key, type);
        }
        Object value = memory.get(compositeKey(region, key));
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return readJson(String.valueOf(value), type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String region, String key, TypeReference<T> typeRef) {
        if (useRedis) {
            return getRedis(region, key, typeRef);
        }
        Object value = memory.get(compositeKey(region, key));
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(value, typeRef);
        } catch (IllegalArgumentException e) {
            LOG.warnf("反序列化缓存失败 typeRef=%s: %s", typeRef.getType(), e.getMessage());
            return null;
        }
    }

    @Override
    public void set(String region, String key, Object value) {
        if (useRedis) {
            setRedis(region, key, value);
            return;
        }
        memory.put(compositeKey(region, key), value);
    }

    @Override
    public void remove(String region, String key) {
        if (useRedis) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.del(compositeKey(region, key));
            }
            return;
        }
        memory.remove(compositeKey(region, key));
    }

    @Override
    public void clearRegion(String region) {
        if (useRedis) {
            clearRedisRegion(region);
            return;
        }
        String prefix = region + ":";
        memory.keySet().removeIf(k -> k.startsWith(prefix));
    }

    @Override
    public Collection<String> keysInRegion(String region) {
        if (useRedis) {
            return keysInRedisRegion(region);
        }
        String prefix = region + ":";
        List<String> keys = new ArrayList<>();
        for (String composite : memory.keySet()) {
            if (composite.startsWith(prefix)) {
                keys.add(composite.substring(prefix.length()));
            }
        }
        return keys;
    }

    private <T> T getRedis(String region, String key, Class<T> type) {
        String json;
        try (Jedis jedis = jedisPool.getResource()) {
            json = jedis.get(compositeKey(region, key));
        }
        if (json == null || json.isBlank()) {
            return null;
        }
        return readJson(json, type);
    }

    private <T> T getRedis(String region, String key, TypeReference<T> typeRef) {
        String json;
        try (Jedis jedis = jedisPool.getResource()) {
            json = jedis.get(compositeKey(region, key));
        }
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            LOG.warnf("反序列化缓存失败 typeRef=%s: %s", typeRef.getType(), e.getMessage());
            return null;
        }
    }

    private void setRedis(String region, String key, Object value) {
        try {
            String json = value instanceof String s ? s : objectMapper.writeValueAsString(value);
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.set(compositeKey(region, key), json);
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化缓存值失败", e);
        }
    }

    private void clearRedisRegion(String region) {
        for (String redisKey : scanRedisRegionKeys(region)) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.del(redisKey);
            }
        }
    }

    private Collection<String> keysInRedisRegion(String region) {
        String prefix = region + ":";
        List<String> keys = new ArrayList<>();
        for (String redisKey : scanRedisRegionKeys(region)) {
            keys.add(redisKey.substring(prefix.length()));
        }
        return keys;
    }

    private List<String> scanRedisRegionKeys(String region) {
        String pattern = region + ":*";
        List<String> keys = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            ScanParams params = new ScanParams().match(pattern).count(100);
            String cursor = ScanParams.SCAN_POINTER_START;
            do {
                ScanResult<String> scan = jedis.scan(cursor, params);
                cursor = scan.getCursor();
                keys.addAll(scan.getResult());
            } while (!ScanParams.SCAN_POINTER_START.equals(cursor));
        }
        return keys;
    }

    private <T> T readJson(String json, Class<T> type) {
        if (type == String.class) {
            return type.cast(json);
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            LOG.warnf("反序列化缓存失败 type=%s: %s", type.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private static String compositeKey(String region, String key) {
        return region + ":" + key;
    }

    private static JedisPool createPool(String hosts) {
        URI uri = URI.create(hosts.contains("://") ? hosts : "redis://" + hosts);
        String host = uri.getHost() != null ? uri.getHost() : "127.0.0.1";
        int port = uri.getPort() > 0 ? uri.getPort() : 6379;
        return new JedisPool(new JedisPoolConfig(), host, port);
    }
}
