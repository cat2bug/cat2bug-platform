package com.cat2bug.common.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallTemplateLoaderTest
{
    @Test
    void loadH2Template_containsDatabaseTypeAndDruidPool()
    {
        Map<String, Object> template = InstallTemplateLoader.loadH2Template();

        assertEquals("h2", nested(template, "spring", "database-type"));
        assertNotNull(nested(template, "spring", "datasource", "druid", "initialSize"));
        assertNotNull(nested(template, "spring", "datasource", "druid", "master", "url"));
    }

    @Test
    void loadMysqlTemplate_containsRedisAndDatabaseType()
    {
        Map<String, Object> template = InstallTemplateLoader.loadMysqlTemplate();

        assertEquals("mysql", nested(template, "spring", "database-type"));
        assertNotNull(nested(template, "spring", "redis", "host"));
    }

    @Test
    void flattenToProperties_producesDotKeys()
    {
        Map<String, Object> flat = InstallTemplateLoader.flattenToProperties(InstallTemplateLoader.loadH2Template());

        assertEquals("h2", flat.get("spring.database-type"));
        assertTrue(flat.containsKey("spring.datasource.druid.maxActive"));
    }

    @Test
    void loadTemplate_rejectsUnknownDatabaseType()
    {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> InstallTemplateLoader.loadTemplate("postgres"));
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
