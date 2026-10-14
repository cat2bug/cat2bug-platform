package com.cat2bug.admin.service;

import com.cat2bug.common.constant.CacheConstants;
import com.cat2bug.framework.cache.AppCache;
import com.cat2bug.framework.security.SessionStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.lang.management.ManagementFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 缓存监控：前端以「缓存名前缀 + 键」展示，这里把前缀映射到实际存储区域。
 */
@ApplicationScoped
public class CacheMonitorService {

    private static final Logger LOG = Logger.getLogger(CacheMonitorService.class);

    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";
    public static final String RATE_LIMIT_KEY = "rate_limit:";
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";

    /** cacheName → AppCache 区域；键在区域内是否已带 cacheName 前缀。 */
    private record Region(String name, boolean keyHasPrefix) {
    }

    private static final Map<String, Region> APP_REGIONS = Map.of(
            CacheConstants.SYS_CONFIG_KEY, new Region("config", false),
            CacheConstants.SYS_DICT_KEY, new Region("dict", false),
            CacheConstants.CAPTCHA_CODE_KEY, new Region("captcha", true));

    private static final List<String[]> CACHE_NAMES = List.of(
            new String[]{CacheConstants.LOGIN_TOKEN_KEY, "用户信息"},
            new String[]{CacheConstants.SYS_CONFIG_KEY, "配置信息"},
            new String[]{CacheConstants.SYS_DICT_KEY, "数据字典"},
            new String[]{CacheConstants.CAPTCHA_CODE_KEY, "验证码"},
            new String[]{REPEAT_SUBMIT_KEY, "防重提交"},
            new String[]{RATE_LIMIT_KEY, "限流处理"},
            new String[]{PWD_ERR_CNT_KEY, "密码错误次数"});

    @Inject
    AppCache appCache;

    @Inject
    SessionStore sessionStore;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "cat2bug.redis.hosts")
    Optional<String> redisHosts;

    private volatile JedisPool jedisPool;

    @PreDestroy
    void shutdown() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

    public List<Map<String, Object>> cacheNames() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String[] item : CACHE_NAMES) {
            list.add(sysCache(item[0], "", "", item[1]));
        }
        return list;
    }

    public List<String> keys(String cacheName) {
        List<String> keys = new ArrayList<>();
        if (CacheConstants.LOGIN_TOKEN_KEY.equals(cacheName)) {
            keys.addAll(sessionStore.listKeys(CacheConstants.LOGIN_TOKEN_KEY));
            return keys;
        }
        Region region = APP_REGIONS.get(cacheName);
        if (region == null) {
            return keys;
        }
        for (String key : appCache.keysInRegion(region.name())) {
            keys.add(region.keyHasPrefix() ? key : cacheName + key);
        }
        return keys;
    }

    public Map<String, Object> value(String cacheName, String cacheKey) {
        Object value = null;
        if (cacheKey.startsWith(CacheConstants.LOGIN_TOKEN_KEY)) {
            value = sessionStore.get(cacheKey);
        } else {
            String prefix = prefixOf(cacheKey);
            Region region = prefix != null ? APP_REGIONS.get(prefix) : null;
            if (region != null) {
                value = appCache.get(region.name(), regionKey(prefix, region, cacheKey), Object.class);
            }
        }
        return sysCache(cacheName, cacheKey, toText(value), "");
    }

    public void clearCacheName(String cacheName) {
        if (CacheConstants.LOGIN_TOKEN_KEY.equals(cacheName)) {
            sessionStore.listKeys(CacheConstants.LOGIN_TOKEN_KEY).forEach(sessionStore::delete);
            return;
        }
        Region region = APP_REGIONS.get(cacheName);
        if (region != null) {
            appCache.clearRegion(region.name());
        }
    }

    public void clearCacheKey(String cacheKey) {
        if (cacheKey.startsWith(CacheConstants.LOGIN_TOKEN_KEY)) {
            sessionStore.delete(cacheKey);
            return;
        }
        String prefix = prefixOf(cacheKey);
        Region region = prefix != null ? APP_REGIONS.get(prefix) : null;
        if (region != null) {
            appCache.remove(region.name(), regionKey(prefix, region, cacheKey));
        }
    }

    public void clearAll() {
        for (String[] item : CACHE_NAMES) {
            clearCacheName(item[0]);
        }
    }

    public Map<String, Object> info() {
        JedisPool pool = redisPool();
        if (pool != null) {
            try {
                return redisInfo(pool);
            } catch (RuntimeException e) {
                LOG.warnf("读取 Redis 信息失败，改为展示本地缓存: %s", e.getMessage());
            }
        }
        return localInfo();
    }

    private Map<String, Object> redisInfo(JedisPool pool) {
        try (Jedis jedis = pool.getResource()) {
            Map<String, String> info = parseInfo(jedis.info());
            Map<String, String> commandStats = parseInfo(jedis.info("commandstats"));
            List<Map<String, String>> pie = new ArrayList<>();
            commandStats.forEach((key, property) -> {
                Map<String, String> data = new LinkedHashMap<>();
                data.put("name", key.startsWith("cmdstat_") ? key.substring("cmdstat_".length()) : key);
                data.put("value", between(property, "calls=", ",usec"));
                pie.add(data);
            });
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("info", info);
            result.put("dbSize", jedis.dbSize());
            result.put("commandStats", pie);
            return result;
        }
    }

    private Map<String, Object> localInfo() {
        Runtime runtime = Runtime.getRuntime();
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
        Map<String, String> info = new LinkedHashMap<>();
        info.put("redis_version", "本地内存");
        info.put("redis_mode", "standalone");
        info.put("tcp_port", "-");
        info.put("connected_clients", String.valueOf(sessionStore.listKeys(CacheConstants.LOGIN_TOKEN_KEY).size()));
        info.put("uptime_in_days", String.valueOf(uptimeMillis / (24L * 60 * 60 * 1000)));
        info.put("used_memory_human", megabytes(runtime.totalMemory() - runtime.freeMemory()));
        info.put("used_cpu_user_children", "0");
        info.put("maxmemory_human", megabytes(runtime.maxMemory()));
        info.put("aof_enabled", "0");
        info.put("rdb_last_bgsave_status", "-");
        info.put("instantaneous_input_kbps", "0");
        info.put("instantaneous_output_kbps", "0");
        List<Map<String, Object>> pie = new ArrayList<>();
        long total = 0;
        for (String[] item : CACHE_NAMES) {
            int size = keys(item[0]).size();
            total += size;
            if (size > 0) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("name", item[0].replace(":", ""));
                data.put("value", size);
                pie.add(data);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("info", info);
        result.put("dbSize", total);
        result.put("commandStats", pie);
        return result;
    }

    private JedisPool redisPool() {
        String hosts = redisHosts.filter(s -> !s.isBlank()).orElse(null);
        if (hosts == null) {
            return null;
        }
        if (jedisPool == null) {
            synchronized (this) {
                if (jedisPool == null) {
                    URI uri = URI.create(hosts.contains("://") ? hosts : "redis://" + hosts);
                    String host = uri.getHost() != null ? uri.getHost() : "127.0.0.1";
                    int port = uri.getPort() > 0 ? uri.getPort() : 6379;
                    jedisPool = new JedisPool(new JedisPoolConfig(), host, port);
                }
            }
        }
        return jedisPool;
    }

    private static Map<String, String> parseInfo(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        if (text == null) {
            return map;
        }
        for (String line : text.split("\r?\n")) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            int idx = line.indexOf(':');
            if (idx > 0) {
                map.put(line.substring(0, idx), line.substring(idx + 1).trim());
            }
        }
        return map;
    }

    private static String between(String text, String open, String close) {
        int start = text.indexOf(open);
        if (start < 0) {
            return "0";
        }
        start += open.length();
        int end = text.indexOf(close, start);
        return end < 0 ? text.substring(start) : text.substring(start, end);
    }

    private static String prefixOf(String cacheKey) {
        int idx = cacheKey.indexOf(':');
        return idx < 0 ? null : cacheKey.substring(0, idx + 1);
    }

    private static String regionKey(String prefix, Region region, String cacheKey) {
        return region.keyHasPrefix() ? cacheKey : cacheKey.substring(prefix.length());
    }

    private String toText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private static String megabytes(long bytes) {
        return String.format("%.2fM", bytes / 1024.0 / 1024.0);
    }

    private static Map<String, Object> sysCache(String cacheName, String cacheKey, String cacheValue, String remark) {
        Map<String, Object> cache = new LinkedHashMap<>();
        cache.put("cacheName", cacheName);
        cache.put("cacheKey", cacheKey);
        cache.put("cacheValue", cacheValue);
        cache.put("remark", remark);
        return cache;
    }
}
