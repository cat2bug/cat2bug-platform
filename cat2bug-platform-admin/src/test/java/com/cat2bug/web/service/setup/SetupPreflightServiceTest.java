package com.cat2bug.web.service.setup;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SetupPreflightServiceTest
{
    @InjectMocks
    private SetupPreflightService setupPreflightService;

    private StandardEnvironment environment;

    @BeforeEach
    void setUp()
    {
        environment = new StandardEnvironment();
        ReflectionTestUtils.setField(setupPreflightService, "environment", environment);
    }

    @Test
    void buildPreflight_exportsMysqlAndRedisFromEnvironment()
    {
        Map<String, Object> props = new HashMap<>();
        props.put("spring.database-type", "mysql");
        props.put("spring.datasource.druid.master.url",
                "jdbc:mysql://cat2bug-platform-mysql:3306/cat2bug_platform?useSSL=false");
        props.put("spring.datasource.druid.master.username", "root");
        props.put("spring.datasource.druid.master.password", "cat2bug_password");
        props.put("cat2bug.cache.type", "redis");
        props.put("spring.redis.host", "cat2bug-platform-redis");
        props.put("spring.redis.port", 6379);
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        Map<String, Object> preflight = setupPreflightService.buildPreflight();

        assertEquals("mysql", preflight.get("databaseType"));
        assertEquals("redis", preflight.get("cacheType"));
        assertEquals("cat2bug-platform-mysql", preflight.get("mysqlHost"));
        assertEquals(3306, preflight.get("mysqlPort"));
        assertEquals("cat2bug_platform", preflight.get("mysqlDatabase"));
        assertEquals("root", preflight.get("mysqlUsername"));
        assertEquals("cat2bug_password", preflight.get("mysqlPassword"));
        assertEquals("cat2bug-platform-redis", preflight.get("redisHost"));
        assertEquals(6379, preflight.get("redisPort"));
    }

    @Test
    void buildPreflight_partialEnvironmentOnlyOverridesProvidedFields()
    {
        Map<String, Object> props = new HashMap<>();
        props.put("spring.redis.host", "redis.internal");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        Map<String, Object> preflight = setupPreflightService.buildPreflight();

        assertFalse(preflight.containsKey("databaseType"));
        assertEquals("redis", preflight.get("cacheType"));
        assertEquals("redis.internal", preflight.get("redisHost"));
        assertNull(preflight.get("mysqlHost"));
    }

    @Test
    void buildPreflight_infersMysqlFromJdbcUrlWithoutDatabaseType()
    {
        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.druid.master.url",
                "jdbc:mysql://mysql:3306/cat2bug_platform");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        Map<String, Object> preflight = setupPreflightService.buildPreflight();

        assertEquals("mysql", preflight.get("databaseType"));
        assertEquals("mysql", preflight.get("mysqlHost"));
        assertEquals(3306, preflight.get("mysqlPort"));
        assertEquals("cat2bug_platform", preflight.get("mysqlDatabase"));
    }

    @Test
    void buildPreflight_infersMysqlFromJdbcUrlDespiteDefaultH2Type()
    {
        Map<String, Object> props = new HashMap<>();
        props.put("spring.database-type", "h2");
        props.put("spring.datasource.druid.master.url",
                "jdbc:mysql://cat2bug-platform-mysql:3306/cat2bug_platform");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        Map<String, Object> preflight = setupPreflightService.buildPreflight();

        assertEquals("mysql", preflight.get("databaseType"));
        assertEquals("cat2bug-platform-mysql", preflight.get("mysqlHost"));
    }

    @Test
    void buildPreflight_dockerDefaultsWhenCat2bugInDocker()
    {
        Map<String, Object> props = new HashMap<>();
        props.put("CAT2BUG_IN_DOCKER", "true");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        Map<String, Object> preflight = setupPreflightService.buildPreflight();

        assertEquals("mysql", preflight.get("databaseType"));
        assertEquals("redis", preflight.get("cacheType"));
        assertEquals("mysql", preflight.get("mysqlHost"));
        assertEquals(3306, preflight.get("mysqlPort"));
        assertEquals("cat2bug_platform", preflight.get("mysqlDatabase"));
        assertEquals("redis", preflight.get("redisHost"));
        assertEquals(6379, preflight.get("redisPort"));
    }

    @Test
    void buildPreflight_environmentOverridesDockerServiceNames()
    {
        Map<String, Object> props = new HashMap<>();
        props.put("CAT2BUG_IN_DOCKER", "true");
        props.put("spring.datasource.druid.master.url",
                "jdbc:mysql://cat2bug-platform-mysql:3306/cat2bug_platform");
        props.put("spring.redis.host", "cat2bug-platform-redis");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        Map<String, Object> preflight = setupPreflightService.buildPreflight();

        assertEquals("cat2bug-platform-mysql", preflight.get("mysqlHost"));
        assertEquals("cat2bug-platform-redis", preflight.get("redisHost"));
    }

    @Test
    void buildPreflight_emptyWhenNoInstallRelatedEnvironment()
    {
        Assumptions.assumeFalse(Files.exists(Paths.get("/.dockerenv")),
                "skipped in container: /.dockerenv enables docker compose defaults");

        Map<String, Object> preflight = setupPreflightService.buildPreflight();
        assertTrue(preflight.isEmpty());
    }
}
