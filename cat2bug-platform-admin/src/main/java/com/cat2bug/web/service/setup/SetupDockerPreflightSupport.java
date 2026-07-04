package com.cat2bug.web.service.setup;

import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Docker Compose 首次安装向导：在环境变量/配置未提供时，预填 compose 服务名（mysql、redis）。
 */
final class SetupDockerPreflightSupport
{
    private SetupDockerPreflightSupport()
    {
    }

    static void applyDefaults(
            Map<String, Object> result,
            Environment environment,
            String inferredDatabaseType,
            String inferredCacheType)
    {
        if (!isDockerRuntime(environment))
        {
            return;
        }
        if (isExplicitDatabaseType(environment, "h2"))
        {
            if (inferredDatabaseType != null)
            {
                result.putIfAbsent("databaseType", inferredDatabaseType);
            }
        }
        else
        {
            result.putIfAbsent("databaseType", inferredDatabaseType != null ? inferredDatabaseType : "mysql");
        }

        if (isExplicitCacheType(environment, "local"))
        {
            if (inferredCacheType != null)
            {
                result.putIfAbsent("cacheType", inferredCacheType);
            }
        }
        else
        {
            result.putIfAbsent("cacheType", inferredCacheType != null ? inferredCacheType : "redis");
        }

        applyMysqlDefaults(result, environment);
        applyRedisDefaults(result, environment);
    }

    static void applyHostDefaults(Map<String, Object> result, Environment environment)
    {
        if (!isDockerRuntime(environment))
        {
            return;
        }
        applyMysqlDefaults(result, environment);
        applyRedisDefaults(result, environment);
    }

    private static void applyMysqlDefaults(Map<String, Object> result, Environment environment)
    {
        Object databaseType = result.get("databaseType");
        if (databaseType == null || !"mysql".equalsIgnoreCase(String.valueOf(databaseType)))
        {
            return;
        }
        result.putIfAbsent("mysqlHost", firstNonBlank(
                environment.getProperty("CAT2BUG_SETUP_MYSQL_HOST"),
                "mysql"));
        result.putIfAbsent("mysqlPort", 3306);
        result.putIfAbsent("mysqlDatabase", firstNonBlank(
                environment.getProperty("CAT2BUG_SETUP_MYSQL_DATABASE"),
                "cat2bug_platform"));
    }

    private static void applyRedisDefaults(Map<String, Object> result, Environment environment)
    {
        Object cacheType = result.get("cacheType");
        if (cacheType == null || !"redis".equalsIgnoreCase(String.valueOf(cacheType)))
        {
            return;
        }
        result.putIfAbsent("redisHost", firstNonBlank(
                environment.getProperty("CAT2BUG_SETUP_REDIS_HOST"),
                "redis"));
        result.putIfAbsent("redisPort", 6379);
    }

    static boolean isDockerRuntime(Environment environment)
    {
        if ("true".equalsIgnoreCase(environment.getProperty("CAT2BUG_IN_DOCKER")))
        {
            return true;
        }
        String envFlag = System.getenv("CAT2BUG_IN_DOCKER");
        if ("true".equalsIgnoreCase(envFlag))
        {
            return true;
        }
        return Files.exists(Paths.get("/.dockerenv"));
    }

    private static boolean isExplicitDatabaseType(Environment environment, String type)
    {
        String value = environment.getProperty("spring.database-type");
        return value != null && type.equalsIgnoreCase(value.trim());
    }

    private static boolean isExplicitCacheType(Environment environment, String type)
    {
        String value = environment.getProperty("cat2bug.cache.type");
        return value != null && type.equalsIgnoreCase(value.trim());
    }

    private static String firstNonBlank(String... candidates)
    {
        for (String candidate : candidates)
        {
            if (candidate != null && !candidate.isBlank())
            {
                return candidate.trim();
            }
        }
        return "";
    }
}
