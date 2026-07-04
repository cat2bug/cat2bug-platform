package com.cat2bug.web.service.setup;

import com.cat2bug.common.config.InstallStartupSupport;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SetupEnvironmentPreflightSupportTest
{
    @Test
    void resolveProperty_prefersProcessEnvironmentOverBootstrapSpringProperty()
    {
        StandardEnvironment environment = new StandardEnvironment();
        Map<String, Object> bootstrap = new HashMap<>();
        bootstrap.put("spring.database-type", "h2");
        bootstrap.put("cat2bug.cache.type", "local");
        bootstrap.put("spring.datasource.druid.master.url", InstallStartupSupport.BOOTSTRAP_H2_JDBC_URL);
        environment.getPropertySources().addFirst(new MapPropertySource("bootstrap", bootstrap));

        Map<String, String> processEnv = Map.of(
                "SPRING_DATABASE_TYPE", "mysql",
                "CAT2BUG_CACHE_TYPE", "redis",
                "SPRING_DATASOURCE_DRUID_MASTER_URL",
                "jdbc:mysql://mysql:3306/cat2bug_platform?useSSL=false",
                "SPRING_REDIS_HOST", "redis");

        assertEquals("mysql", SetupEnvironmentPreflightSupport.resolveProperty(
                environment, processEnv::get, "spring.database-type", "SPRING_DATABASE_TYPE"));
        assertEquals("redis", SetupEnvironmentPreflightSupport.resolveProperty(
                environment, processEnv::get, "cat2bug.cache.type", "CAT2BUG_CACHE_TYPE"));
        assertEquals("jdbc:mysql://mysql:3306/cat2bug_platform?useSSL=false",
                SetupEnvironmentPreflightSupport.resolveProperty(
                        environment,
                        processEnv::get,
                        "spring.datasource.druid.master.url",
                        "SPRING_DATASOURCE_DRUID_MASTER_URL",
                        "SPRING_DATASOURCE_URL"));
        assertEquals("redis", SetupEnvironmentPreflightSupport.resolveProperty(
                environment, processEnv::get, "spring.redis.host", "SPRING_REDIS_HOST"));
    }

    @Test
    void applyExplicitConfiguration_populatesMysqlAndRedisHostsFromComposeEnv()
    {
        StandardEnvironment environment = new StandardEnvironment();
        Map<String, Object> bootstrap = new HashMap<>();
        bootstrap.put("spring.database-type", "h2");
        bootstrap.put("cat2bug.cache.type", "local");
        bootstrap.put("spring.datasource.druid.master.url", InstallStartupSupport.BOOTSTRAP_H2_JDBC_URL);
        environment.getPropertySources().addFirst(new MapPropertySource("bootstrap", bootstrap));

        Map<String, String> processEnv = Map.of(
                "SPRING_DATABASE_TYPE", "mysql",
                "CAT2BUG_CACHE_TYPE", "redis",
                "SPRING_DATASOURCE_DRUID_MASTER_URL",
                "jdbc:mysql://mysql:3306/cat2bug_platform?useSSL=false",
                "SPRING_DATASOURCE_DRUID_MASTER_USERNAME", "root",
                "SPRING_DATASOURCE_DRUID_MASTER_PASSWORD", "cat2bug_password",
                "SPRING_REDIS_HOST", "redis",
                "SPRING_REDIS_PORT", "6379");

        Map<String, Object> result = new LinkedHashMap<>();
        SetupEnvironmentPreflightSupport.applyExplicitConfiguration(
                result,
                environment,
                processEnv::get);

        assertEquals("mysql", result.get("databaseType"));
        assertEquals("redis", result.get("cacheType"));
        assertEquals("mysql", result.get("mysqlHost"));
        assertEquals(3306, result.get("mysqlPort"));
        assertEquals("cat2bug_platform", result.get("mysqlDatabase"));
        assertEquals("root", result.get("mysqlUsername"));
        assertEquals("cat2bug_password", result.get("mysqlPassword"));
        assertEquals("redis", result.get("redisHost"));
        assertEquals(6379, result.get("redisPort"));
    }
}
