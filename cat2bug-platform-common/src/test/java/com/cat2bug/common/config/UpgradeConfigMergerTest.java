package com.cat2bug.common.config;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UpgradeConfigMergerTest
{
    @Test
    void mergePreserveExisting_keepsCustomJdbcUrlFromBase()
    {
        Map<String, Object> template = datasourceRoot("jdbc:mysql://template:3306/db", "templateUser");
        Map<String, Object> base = datasourceRoot("jdbc:mysql://127.0.0.1:3306/cat2bug_platform", "root");
        Map<String, Object> merged = UpgradeConfigMerger.mergePreserveExisting(base, template, null);

        assertEquals("jdbc:mysql://127.0.0.1:3306/cat2bug_platform",
                nested(merged, "spring", "datasource", "druid", "master", "url"));
        assertEquals("root", nested(merged, "spring", "datasource", "druid", "master", "username"));
    }

    @Test
    void mergePreserveExisting_fillsMissingCacheTypeFromTemplate()
    {
        Map<String, Object> template = new LinkedHashMap<>();
        putNested(template, new String[]{"cat2bug", "cache", "type"}, "local");
        Map<String, Object> base = new LinkedHashMap<>();
        putNested(base, new String[]{"spring", "database-type"}, "mysql");

        Map<String, Object> merged = UpgradeConfigMerger.mergePreserveExisting(base, template, null);

        assertEquals("mysql", nested(merged, "spring", "database-type"));
        assertEquals("local", nested(merged, "cat2bug", "cache", "type"));
    }

    @Test
    void mergePreserveExisting_wizardOverrideReplacesBaseValue()
    {
        Map<String, Object> template = redisHost("127.0.0.1");
        Map<String, Object> base = redisHost("192.168.1.10");
        Map<String, Object> wizard = redisHost("10.0.0.5");

        Map<String, Object> merged = UpgradeConfigMerger.mergePreserveExisting(base, template, wizard);

        assertEquals("10.0.0.5", nested(merged, "spring", "redis", "host"));
    }

    @Test
    void mergePreserveExisting_blankBaseDoesNotOverrideTemplate()
    {
        Map<String, Object> template = profileRoot("/data/template");
        Map<String, Object> base = profileRoot("   ");

        Map<String, Object> merged = UpgradeConfigMerger.mergePreserveExisting(base, template, null);

        assertEquals("/data/template", nested(merged, "cat2bug", "profile"));
    }

    @Test
    void mergePreserveExisting_wizardOverridesTemplateAndBase()
    {
        Map<String, Object> template = profileRoot("/data/template");
        Map<String, Object> base = profileRoot("/data/legacy");
        Map<String, Object> wizard = profileRoot("/data/user-chosen");

        Map<String, Object> merged = UpgradeConfigMerger.mergePreserveExisting(base, template, wizard);

        assertEquals("/data/user-chosen", nested(merged, "cat2bug", "profile"));
    }

    private static Map<String, Object> datasourceRoot(String url, String username)
    {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> master = new LinkedHashMap<>();
        master.put("url", url);
        master.put("username", username);
        Map<String, Object> druid = new LinkedHashMap<>();
        druid.put("master", master);
        Map<String, Object> datasource = new LinkedHashMap<>();
        datasource.put("druid", druid);
        Map<String, Object> spring = new LinkedHashMap<>();
        spring.put("datasource", datasource);
        root.put("spring", spring);
        return root;
    }

    private static Map<String, Object> redisHost(String host)
    {
        Map<String, Object> redis = new LinkedHashMap<>();
        redis.put("host", host);
        Map<String, Object> spring = new LinkedHashMap<>();
        spring.put("redis", redis);
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("spring", spring);
        return root;
    }

    private static Map<String, Object> profileRoot(String profile)
    {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> cat2bug = new LinkedHashMap<>();
        cat2bug.put("profile", profile);
        root.put("cat2bug", cat2bug);
        return root;
    }

    private static void putNested(Map<String, Object> root, String[] path, Object value)
    {
        Map<String, Object> current = root;
        for (int i = 0; i < path.length - 1; i++)
        {
            current = (Map<String, Object>) current.computeIfAbsent(path[i], k -> new LinkedHashMap<>());
        }
        current.put(path[path.length - 1], value);
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
