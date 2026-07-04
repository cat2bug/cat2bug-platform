package com.cat2bug.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallConfigExporterTest
{
    @Test
    void shouldExport_installRelatedKeysOnly()
    {
        assertTrue(InstallConfigExporter.shouldExport("spring.database-type"));
        assertTrue(InstallConfigExporter.shouldExport("spring.datasource.druid.master.url"));
        assertTrue(InstallConfigExporter.shouldExport("cat2bug.cache.type"));
        assertFalse(InstallConfigExporter.shouldExport("cat2bug.install.completed"));
        assertFalse(InstallConfigExporter.shouldExport("spring.profiles.active"));
    }

    @Test
    void exportFromEnvironment_resolvesUppercaseEnvironmentVariables()
    {
        StandardEnvironment environment = new StandardEnvironment();
        Map<String, Object> props = new HashMap<>();
        props.put("SPRING_REDIS_HOST", "redis");
        props.put("SPRING_REDIS_PORT", "6379");
        props.put("SPRING_DATASOURCE_DRUID_MASTER_URL",
                "jdbc:mysql://mysql:3306/cat2bug_platform?useSSL=false");
        environment.getPropertySources().addFirst(
                new org.springframework.core.env.SystemEnvironmentPropertySource("test-env", props));

        Map<String, Object> exported = InstallConfigExporter.exportFromEnvironment(environment);

        assertEquals("redis", nested(exported, "spring", "redis", "host"));
        assertEquals(6379, nested(exported, "spring", "redis", "port"));
        assertTrue(String.valueOf(nested(exported, "spring", "datasource", "druid", "master", "url"))
                .contains("jdbc:mysql://mysql:3306/cat2bug_platform"));
    }

    @Test
    void exportFromEnvironment_buildsNestedStructure()
    {
        StandardEnvironment environment = new StandardEnvironment();
        Map<String, Object> props = new HashMap<>();
        props.put("spring.database-type", "h2");
        props.put("cat2bug.cache.type", "local");
        props.put("cat2bug.install.bootstrap-mode", "true");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        Map<String, Object> exported = InstallConfigExporter.exportFromEnvironment(environment);

        assertEquals("h2", nested(exported, "spring", "database-type"));
        assertEquals("local", nested(exported, "cat2bug", "cache", "type"));
        assertFalse(exported.toString().contains("bootstrap-mode"));
    }

    @SuppressWarnings("unchecked")
    private static Object nested(Map<String, Object> root, String... path)
    {
        Object current = root;
        for (String key : path)
        {
            if (!(current instanceof Map<?, ?> map))
            {
                return null;
            }
            current = ((Map<String, Object>) map).get(key);
        }
        return current;
    }
}
