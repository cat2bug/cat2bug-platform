package com.cat2bug.admin.setup;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class SetupPreflightServiceTest {

    private static SmallRyeConfig config(Map<String, String> props) {
        return new SmallRyeConfigBuilder()
                .withSources(new PropertiesConfigSource(props, "test", 100))
                .build();
    }

    @Test
    void plainHostLeavesTypesForFrontendDefaults() {
        Map<String, Object> result = SetupPreflightService.buildPreflight(
                new HashMap<>(), config(Map.of()), k -> null, false);
        assertFalse(result.containsKey("databaseType"));
        assertFalse(result.containsKey("cacheType"));
    }

    @Test
    void dockerDefaultsToComposeServiceNames() {
        Map<String, String> env = Map.of("CAT2BUG_SETUP_MYSQL_DATABASE", "cat2bug_docker");
        Map<String, Object> result = SetupPreflightService.buildPreflight(
                new HashMap<>(), config(Map.of()), env::get, true);
        assertEquals("mysql", result.get("databaseType"));
        assertEquals("mysql", result.get("mysqlHost"));
        assertEquals(3306, result.get("mysqlPort"));
        assertEquals("cat2bug_docker", result.get("mysqlDatabase"));
        assertEquals("redis", result.get("cacheType"));
        assertEquals("redis", result.get("redisHost"));
        assertEquals(6379, result.get("redisPort"));
    }

    @Test
    void environmentVariablesOverrideEverything() {
        Map<String, String> env = Map.of(
                "SPRING_DATASOURCE_URL", "jdbc:mysql://db.internal:3307/cat2bug_x?useSSL=false",
                "SPRING_DATASOURCE_DRUID_MASTER_USERNAME", "u1",
                "SPRING_REDIS_HOST", "cache.internal",
                "SPRING_REDIS_PORT", "6380",
                "CAT2BUG_PROFILE", "/data/cat2bug",
                "CAT2BUG_AI_ENABLED", "true");
        Map<String, Object> result = SetupPreflightService.buildPreflight(
                new HashMap<>(), config(Map.of()), env::get, false);
        assertEquals("mysql", result.get("databaseType"));
        assertEquals("db.internal", result.get("mysqlHost"));
        assertEquals(3307, result.get("mysqlPort"));
        assertEquals("cat2bug_x", result.get("mysqlDatabase"));
        assertEquals("u1", result.get("mysqlUsername"));
        assertEquals("redis", result.get("cacheType"));
        assertEquals("cache.internal", result.get("redisHost"));
        assertEquals(6380, result.get("redisPort"));
        assertEquals("/data/cat2bug", result.get("profile"));
        assertEquals(true, result.get("aiEnabled"));
    }

    @Test
    void explicitH2InDockerKeepsDatabaseTypeUnset() {
        Map<String, Object> result = SetupPreflightService.buildPreflight(
                new HashMap<>(), config(Map.of("spring.database-type", "h2", "cat2bug.cache.type", "local")),
                k -> null, true);
        assertEquals("h2", result.get("databaseType"));
        assertEquals("local", result.get("cacheType"));
        assertNull(result.get("mysqlHost"));
    }
}
