package com.cat2bug.quarkus.admin.setup;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * 首次安装向导预检（与 Spring {@code SetupPreflightService} 对等）：从运行期配置、Docker 环境与
 * compose/K8s 注入的环境变量推断并预填表单（数据库/缓存类型、MySQL/Redis 连接、存储路径、AI）。
 */
@ApplicationScoped
public class SetupPreflightService {

    @Inject
    QuarkusInstallConfigExporter installConfigExporter;

    public Map<String, Object> buildPreflight() {
        Map<String, Object> base = installConfigExporter.exportFromRuntime();
        return buildPreflight(base, ConfigProvider.getConfig(), System::getenv, isDockerRuntime());
    }

    static Map<String, Object> buildPreflight(Map<String, Object> base, Config config,
                                              Function<String, String> env, boolean docker) {
        if (base == null) {
            base = new LinkedHashMap<>();
        }
        String databaseType = resolveInferredDatabaseType(base);
        String cacheType = resolveInferredCacheType(base);

        Map<String, Object> result = new LinkedHashMap<>();
        putFormFields(result, base,
                databaseType != null ? databaseType : defaultIfNull(readDatabaseType(base), "h2"),
                cacheType != null ? cacheType : defaultIfNull(readCacheType(base), "local"));
        if (databaseType != null) {
            result.putIfAbsent("databaseType", databaseType);
        }
        if (cacheType != null) {
            result.putIfAbsent("cacheType", cacheType);
        }
        if (docker) {
            applyDockerDefaults(result, config, env, databaseType, cacheType);
        }
        applyExplicitConfiguration(result, config, env);
        return result;
    }

    // ---------- install 配置树 → 表单 ----------

    static void putFormFields(Map<String, Object> result, Map<String, Object> base, String databaseType, String cacheType) {
        putOptionalString(result, "profile", stringValue(nested(base, "cat2bug", "profile")));
        putOptionalString(result, "temp", stringValue(nested(base, "cat2bug", "temp")));
        putOptionalString(result, "logPath", stringValue(nested(base, "logging", "file", "path")));
        if (nested(base, "cat2bug", "ai") instanceof Map<?, ?> ai) {
            if (ai.containsKey("enabled")) {
                result.put("aiEnabled", Boolean.TRUE.equals(ai.get("enabled")));
            }
            putOptionalString(result, "aiHost", stringValue(ai.get("host")));
        }
        if ("mysql".equalsIgnoreCase(databaseType)
                && nested(base, "spring", "datasource", "druid", "master") instanceof Map<?, ?> master) {
            putOptionalString(result, "mysqlUsername", stringValue(master.get("username")));
            putOptionalString(result, "mysqlPassword", stringValue(master.get("password")));
            parseMysqlJdbcUrl(stringValue(master.get("url")), result);
        }
        if ("redis".equalsIgnoreCase(cacheType) && nested(base, "spring", "redis") instanceof Map<?, ?> redis) {
            putOptionalString(result, "redisHost", stringValue(redis.get("host")));
            if (redis.get("port") instanceof Number port) {
                result.put("redisPort", port.intValue());
            }
            putOptionalString(result, "redisPassword", stringValue(redis.get("password")));
            if (redis.get("database") instanceof Number db) {
                result.put("redisDatabase", db.intValue());
            }
        }
    }

    static void parseMysqlJdbcUrl(String jdbcUrl, Map<String, Object> target) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return;
        }
        int schemeEnd = jdbcUrl.indexOf("://");
        if (schemeEnd < 0) {
            return;
        }
        String remainder = jdbcUrl.substring(schemeEnd + 3);
        int slash = remainder.indexOf('/');
        String authority = slash >= 0 ? remainder.substring(0, slash) : remainder;
        String path = slash >= 0 ? remainder.substring(slash + 1) : "";
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        int colon = authority.indexOf(':');
        if (colon >= 0) {
            target.put("mysqlHost", authority.substring(0, colon));
            try {
                target.put("mysqlPort", Integer.parseInt(authority.substring(colon + 1)));
            } catch (NumberFormatException ignored) {
                target.put("mysqlPort", 3306);
            }
        } else {
            target.put("mysqlHost", authority);
            target.put("mysqlPort", 3306);
        }
        if (!path.isBlank()) {
            target.put("mysqlDatabase", path);
        }
    }

    static String resolveInferredDatabaseType(Map<String, Object> base) {
        if (stringValue(nested(base, "spring", "datasource", "druid", "master", "url")).contains("jdbc:mysql")) {
            return "mysql";
        }
        return readDatabaseType(base);
    }

    static String resolveInferredCacheType(Map<String, Object> base) {
        String explicit = readCacheType(base);
        if (explicit != null) {
            return explicit;
        }
        return stringValue(nested(base, "spring", "redis", "host")).isBlank() ? null : "redis";
    }

    private static String readDatabaseType(Map<String, Object> base) {
        String value = stringValue(nested(base, "spring", "database-type"));
        return value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String readCacheType(Map<String, Object> base) {
        String value = stringValue(nested(base, "cat2bug", "cache", "type"));
        return value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    // ---------- Docker Compose 默认服务名 ----------

    static boolean isDockerRuntime() {
        return "true".equalsIgnoreCase(System.getenv("CAT2BUG_IN_DOCKER"))
                || "true".equalsIgnoreCase(ConfigProvider.getConfig()
                .getOptionalValue("CAT2BUG_IN_DOCKER", String.class).orElse(null))
                || Files.exists(Paths.get("/.dockerenv"));
    }

    private static void applyDockerDefaults(Map<String, Object> result, Config config, Function<String, String> env,
                                            String inferredDatabaseType, String inferredCacheType) {
        if (isExplicit(config, "spring.database-type", "h2")) {
            if (inferredDatabaseType != null) {
                result.putIfAbsent("databaseType", inferredDatabaseType);
            }
        } else {
            result.putIfAbsent("databaseType", inferredDatabaseType != null ? inferredDatabaseType : "mysql");
        }
        if (isExplicit(config, "cat2bug.cache.type", "local")) {
            if (inferredCacheType != null) {
                result.putIfAbsent("cacheType", inferredCacheType);
            }
        } else {
            result.putIfAbsent("cacheType", inferredCacheType != null ? inferredCacheType : "redis");
        }
        if ("mysql".equalsIgnoreCase(String.valueOf(result.get("databaseType")))) {
            result.putIfAbsent("mysqlHost", firstNonBlank(resolve(config, env, "CAT2BUG_SETUP_MYSQL_HOST"), "mysql"));
            result.putIfAbsent("mysqlPort", 3306);
            result.putIfAbsent("mysqlDatabase",
                    firstNonBlank(resolve(config, env, "CAT2BUG_SETUP_MYSQL_DATABASE"), "cat2bug_platform"));
        }
        if ("redis".equalsIgnoreCase(String.valueOf(result.get("cacheType")))) {
            result.putIfAbsent("redisHost", firstNonBlank(resolve(config, env, "CAT2BUG_SETUP_REDIS_HOST"), "redis"));
            result.putIfAbsent("redisPort", 6379);
        }
    }

    private static boolean isExplicit(Config config, String key, String type) {
        String value = config.getOptionalValue(key, String.class).orElse(null);
        return value != null && type.equalsIgnoreCase(value.trim());
    }

    // ---------- compose / K8s 环境变量（优先级最高） ----------

    static void applyExplicitConfiguration(Map<String, Object> result, Config config, Function<String, String> env) {
        String databaseType = resolve(config, env, "spring.database-type", "SPRING_DATABASE_TYPE");
        if (databaseType != null) {
            result.put("databaseType", databaseType.toLowerCase(Locale.ROOT));
        }
        String cacheType = resolve(config, env, "cat2bug.cache.type", "CAT2BUG_CACHE_TYPE");
        if (cacheType != null) {
            result.put("cacheType", cacheType.toLowerCase(Locale.ROOT));
        }

        String jdbcUrl = resolve(config, env, "spring.datasource.druid.master.url",
                "SPRING_DATASOURCE_DRUID_MASTER_URL", "SPRING_DATASOURCE_URL");
        if (jdbcUrl != null && jdbcUrl.contains("jdbc:mysql")) {
            Map<String, Object> parsed = new LinkedHashMap<>();
            parseMysqlJdbcUrl(jdbcUrl, parsed);
            parsed.forEach((k, v) -> {
                if (v != null && !String.valueOf(v).isBlank()) {
                    result.put(k, v);
                }
            });
            result.put("databaseType", "mysql");
        }
        putIfResolved(result, "mysqlUsername", resolve(config, env,
                "spring.datasource.druid.master.username", "SPRING_DATASOURCE_DRUID_MASTER_USERNAME"));
        putIfResolved(result, "mysqlPassword", resolve(config, env,
                "spring.datasource.druid.master.password", "SPRING_DATASOURCE_DRUID_MASTER_PASSWORD"));

        String redisHost = resolve(config, env, "spring.redis.host", "SPRING_REDIS_HOST");
        if (redisHost != null) {
            result.put("redisHost", redisHost);
            result.put("cacheType", "redis");
        }
        String redisPort = resolve(config, env, "spring.redis.port", "SPRING_REDIS_PORT");
        if (redisPort != null) {
            result.put("redisPort", parseInteger(redisPort));
        }
        putIfResolved(result, "redisPassword", resolve(config, env, "spring.redis.password", "SPRING_REDIS_PASSWORD"));
        String redisDatabase = resolve(config, env, "spring.redis.database", "SPRING_REDIS_DATABASE");
        if (redisDatabase != null) {
            result.put("redisDatabase", parseInteger(redisDatabase));
        }

        putIfResolved(result, "profile", resolve(config, env, "cat2bug.profile", "CAT2BUG_PROFILE"));
        putIfResolved(result, "temp", resolve(config, env, "cat2bug.temp", "CAT2BUG_TEMP"));
        putIfResolved(result, "logPath", resolve(config, env, "logging.file.path", "LOGGING_FILE_PATH"));

        String aiEnabled = resolve(config, env, "cat2bug.ai.enabled", "CAT2BUG_AI_ENABLED");
        if (aiEnabled != null) {
            result.put("aiEnabled", Boolean.parseBoolean(aiEnabled));
        }
        putIfResolved(result, "aiHost", resolve(config, env, "cat2bug.ai.host", "CAT2BUG_AI_HOST"));
    }

    /** 先查进程环境变量，再查配置项（与 Spring 优先级一致）。 */
    static String resolve(Config config, Function<String, String> env, String propertyKey, String... envKeys) {
        for (String key : envKeys) {
            String value = env.apply(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        if (envKeys.length == 0) {
            String value = env.apply(propertyKey);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        String value = config.getOptionalValue(propertyKey, String.class).orElse(null);
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    // ---------- 工具 ----------

    private static void putOptionalString(Map<String, Object> result, String key, String value) {
        if (value != null && !value.isBlank()) {
            result.put(key, value);
        }
    }

    private static void putIfResolved(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return "";
    }

    private static String defaultIfNull(String value, String fallback) {
        return value != null ? value : fallback;
    }

    static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    static Object nested(Map<String, Object> root, String... path) {
        Object current = root;
        for (String key : path) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = ((Map<String, Object>) map).get(key);
        }
        return current;
    }
}
