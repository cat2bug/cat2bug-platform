package com.cat2bug.web.service.setup;

import org.springframework.core.env.Environment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 安装向导预检：直接从 Spring Environment / 进程环境变量读取 compose 注入的配置（优先级最高）。
 */
final class SetupEnvironmentPreflightSupport
{
    private SetupEnvironmentPreflightSupport()
    {
    }

    /**
     * 将 docker-compose / K8s 注入的环境变量写入预检结果（覆盖引导期 H2 默认值）。
     */
    static void applyExplicitConfiguration(Map<String, Object> result, Environment environment)
    {
        String databaseType = resolveProperty(environment, "spring.database-type", "SPRING_DATABASE_TYPE");
        if (databaseType != null)
        {
            result.put("databaseType", databaseType.trim().toLowerCase());
        }

        String cacheType = resolveProperty(environment, "cat2bug.cache.type", "CAT2BUG_CACHE_TYPE");
        if (cacheType != null)
        {
            result.put("cacheType", cacheType.trim().toLowerCase());
        }

        applyMysqlFromEnvironment(result, environment);
        applyRedisFromEnvironment(result, environment);
        applyPathsFromEnvironment(result, environment);
        applyAiFromEnvironment(result, environment);
    }

    private static void applyMysqlFromEnvironment(Map<String, Object> result, Environment environment)
    {
        String jdbcUrl = resolveProperty(environment,
                "spring.datasource.druid.master.url",
                "SPRING_DATASOURCE_DRUID_MASTER_URL",
                "SPRING_DATASOURCE_URL");
        if (jdbcUrl != null && jdbcUrl.contains("jdbc:mysql"))
        {
            Map<String, Object> parsed = new LinkedHashMap<>();
            SetupFormDefaultsSupport.parseMysqlJdbcUrl(jdbcUrl, parsed);
            mergeNonBlank(result, parsed);
            result.put("databaseType", "mysql");
        }

        putIfResolved(result, "mysqlUsername", resolveProperty(environment,
                "spring.datasource.druid.master.username",
                "SPRING_DATASOURCE_DRUID_MASTER_USERNAME"));
        putIfResolved(result, "mysqlPassword", resolveProperty(environment,
                "spring.datasource.druid.master.password",
                "SPRING_DATASOURCE_DRUID_MASTER_PASSWORD"));
    }

    private static void applyRedisFromEnvironment(Map<String, Object> result, Environment environment)
    {
        String redisHost = resolveProperty(environment, "spring.redis.host", "SPRING_REDIS_HOST");
        if (redisHost != null)
        {
            result.put("redisHost", redisHost);
            result.putIfAbsent("cacheType", "redis");
        }

        String redisPort = resolveProperty(environment, "spring.redis.port", "SPRING_REDIS_PORT");
        if (redisPort != null)
        {
            result.put("redisPort", parseInteger(redisPort));
        }

        putIfResolved(result, "redisPassword", resolveProperty(environment,
                "spring.redis.password",
                "SPRING_REDIS_PASSWORD"));

        String redisDatabase = resolveProperty(environment, "spring.redis.database", "SPRING_REDIS_DATABASE");
        if (redisDatabase != null)
        {
            result.put("redisDatabase", parseInteger(redisDatabase));
        }
    }

    private static void applyPathsFromEnvironment(Map<String, Object> result, Environment environment)
    {
        putIfResolved(result, "profile", resolveProperty(environment, "cat2bug.profile", "CAT2BUG_PROFILE"));
        putIfResolved(result, "temp", resolveProperty(environment, "cat2bug.temp", "CAT2BUG_TEMP"));
        putIfResolved(result, "logPath", resolveProperty(environment, "logging.file.path", "LOGGING_FILE_PATH"));
    }

    private static void applyAiFromEnvironment(Map<String, Object> result, Environment environment)
    {
        String aiEnabled = resolveProperty(environment, "cat2bug.ai.enabled", "CAT2BUG_AI_ENABLED");
        if (aiEnabled != null)
        {
            result.put("aiEnabled", Boolean.parseBoolean(aiEnabled.trim()));
        }
        putIfResolved(result, "aiHost", resolveProperty(environment, "cat2bug.ai.host", "CAT2BUG_AI_HOST"));
    }

    private static void mergeNonBlank(Map<String, Object> target, Map<String, Object> source)
    {
        for (Map.Entry<String, Object> entry : source.entrySet())
        {
            if (entry.getValue() != null && !String.valueOf(entry.getValue()).isBlank())
            {
                target.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void putIfResolved(Map<String, Object> target, String key, String value)
    {
        if (value != null && !value.isBlank())
        {
            target.put(key, value);
        }
    }

    private static Integer parseInteger(String value)
    {
        try
        {
            return Integer.parseInt(value.trim());
        }
        catch (NumberFormatException ignored)
        {
            return null;
        }
    }

    static String resolveProperty(Environment environment, String propertyKey, String... environmentKeys)
    {
        String fromProperty = environment.getProperty(propertyKey);
        if (fromProperty != null && !fromProperty.isBlank())
        {
            return fromProperty.trim();
        }
        for (String environmentKey : environmentKeys)
        {
            String fromEnvironment = System.getenv(environmentKey);
            if (fromEnvironment != null && !fromEnvironment.isBlank())
            {
                return fromEnvironment.trim();
            }
        }
        return null;
    }
}
