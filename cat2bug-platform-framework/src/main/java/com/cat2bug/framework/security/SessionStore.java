package com.cat2bug.framework.security;

import com.cat2bug.common.domain.LoginUser;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.resps.ScanResult;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SessionStore {

    private static final Logger LOG = Logger.getLogger(SessionStore.class);
    private static final String REGION = "tokenExpireTime";

    private record MemoryEntry(LoginUser loginUser, long expiresAtEpochSeconds) {
    }

    @ConfigProperty(name = "cat2bug.session.store", defaultValue = "auto")
    String storeMode;

    @ConfigProperty(name = "cat2bug.redis.hosts")
    java.util.Optional<String> redisHosts;

    @Inject
    ObjectMapper objectMapper;

    private final Map<String, MemoryEntry> memory = new ConcurrentHashMap<>();
    private boolean useRedis;
    private JedisPool jedisPool;

    @PostConstruct
    void init() {
        boolean redisConfigured = redisHosts.filter(s -> !s.isBlank()).isPresent();
        if ("redis".equalsIgnoreCase(storeMode)) {
            useRedis = redisConfigured;
            if (!useRedis) {
                throw new IllegalStateException("cat2bug.session.store=redis 但未配置 cat2bug.redis.hosts");
            }
        } else if ("memory".equalsIgnoreCase(storeMode)) {
            useRedis = false;
        } else {
            useRedis = redisConfigured;
        }
        if (useRedis) {
            jedisPool = createPool(redisHosts.orElseThrow());
        }
        LOG.infof("会话存储: %s", useRedis ? "redis" : "memory");
    }

    @PreDestroy
    void shutdown() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

    public void save(String sessionKey, LoginUser loginUser, long ttlSeconds) {
        if (useRedis) {
            saveRedis(sessionKey, loginUser, ttlSeconds);
        } else {
            long expiresAt = (System.currentTimeMillis() / 1000L) + ttlSeconds;
            memory.put(sessionKey, new MemoryEntry(loginUser, expiresAt));
        }
    }

    public LoginUser get(String sessionKey) {
        if (useRedis) {
            return getRedis(sessionKey);
        }
        MemoryEntry entry = memory.get(sessionKey);
        if (entry == null) {
            return null;
        }
        if (entry.expiresAtEpochSeconds() < System.currentTimeMillis() / 1000L) {
            memory.remove(sessionKey);
            return null;
        }
        return entry.loginUser();
    }

    public void delete(String sessionKey) {
        if (useRedis) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.del(redisKey(sessionKey));
            }
        } else {
            memory.remove(sessionKey);
        }
    }

    /** 未过期会话的键（与 {@code save} 时传入的 sessionKey 相同）。 */
    public List<String> listKeys(String sessionKeyPrefix) {
        List<String> keys = new ArrayList<>();
        if (useRedis) {
            ScanParams params = new ScanParams().match(redisKey(sessionKeyPrefix) + "*").count(100);
            try (Jedis jedis = jedisPool.getResource()) {
                String cursor = ScanParams.SCAN_POINTER_START;
                do {
                    ScanResult<String> scan = jedis.scan(cursor, params);
                    cursor = scan.getCursor();
                    for (String key : scan.getResult()) {
                        keys.add(key.substring(REGION.length() + 1));
                    }
                } while (!ScanParams.SCAN_POINTER_START.equals(cursor));
            }
            return keys;
        }
        long now = System.currentTimeMillis() / 1000L;
        memory.entrySet().removeIf(e -> e.getValue().expiresAtEpochSeconds() < now);
        for (String key : memory.keySet()) {
            if (key.startsWith(sessionKeyPrefix)) {
                keys.add(key);
            }
        }
        return keys;
    }

    public List<LoginUser> listActive(String sessionKeyPrefix) {
        List<LoginUser> result = new ArrayList<>();
        for (String key : listKeys(sessionKeyPrefix)) {
            LoginUser user = get(key);
            if (user != null) {
                result.add(user);
            }
        }
        return result;
    }

    private void saveRedis(String sessionKey, LoginUser loginUser, long ttlSeconds) {
        try {
            String json = objectMapper.writeValueAsString(loginUser);
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.set(redisKey(sessionKey), json, SetParams.setParams().ex(ttlSeconds));
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化 LoginUser 失败", e);
        }
    }

    private LoginUser getRedis(String sessionKey) {
        String json;
        try (Jedis jedis = jedisPool.getResource()) {
            json = jedis.get(redisKey(sessionKey));
        }
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            LoginUser user = objectMapper.readValue(json, LoginUser.class);
            // 旧版 Spring 写入的 fastjson（@type/@object）会话会被 Jackson 解析成空对象
            if (user == null || user.getToken() == null) {
                return null;
            }
            Long expireTime = user.getExpireTime();
            if (expireTime != null && expireTime > 0 && expireTime < System.currentTimeMillis()) {
                return null;
            }
            return user;
        } catch (JsonProcessingException e) {
            LOG.warnf("反序列化 LoginUser 失败: %s", e.getMessage());
            return null;
        }
    }

    private static JedisPool createPool(String hosts) {
        URI uri = URI.create(hosts.contains("://") ? hosts : "redis://" + hosts);
        String host = uri.getHost() != null ? uri.getHost() : "127.0.0.1";
        int port = uri.getPort() > 0 ? uri.getPort() : 6379;
        return new JedisPool(new JedisPoolConfig(), host, port);
    }

    /** J2Cache generic 存储格式：{region}:{key} */
    private static String redisKey(String sessionKey) {
        return REGION + ":" + sessionKey;
    }
}
