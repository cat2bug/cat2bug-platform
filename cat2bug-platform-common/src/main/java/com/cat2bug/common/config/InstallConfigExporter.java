package com.cat2bug.common.config;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 从 Spring {@link Environment} 导出安装相关配置树，供 Legacy 迁移使用。
 */
public final class InstallConfigExporter
{
    /**
     * 通过 {@link Environment#getProperty(String)} 解析的安装相关键（含 {@code SPRING_REDIS_HOST} 等环境变量宽松绑定）。
     */
    private static final List<String> RESOLVABLE_INSTALL_PROPERTY_KEYS = List.of(
            "spring.database-type",
            "spring.datasource.druid.master.url",
            "spring.datasource.druid.master.username",
            "spring.datasource.druid.master.password",
            "spring.redis.host",
            "spring.redis.port",
            "spring.redis.password",
            "spring.redis.database",
            "cat2bug.cache.type",
            "cat2bug.profile",
            "cat2bug.temp",
            "cat2bug.ai.enabled",
            "cat2bug.ai.host",
            "logging.file.path");

    private InstallConfigExporter()
    {
    }

    /**
     * 导出当前 Environment 中的基础设施配置，并基于 classpath 模板补全骨架。
     */
    public static Map<String, Object> exportForMigration(Environment environment)
    {
        String databaseType = environment.getProperty("spring.database-type", "h2");
        Map<String, Object> root = InstallConfigMerge.deepCopy(InstallTemplateLoader.loadTemplate(databaseType));
        Map<String, Object> exported = exportFromEnvironment(environment);
        InstallConfigMerge.deepMerge(root, exported);
        return root;
    }

    /**
     * 将 Environment 中匹配的安装相关属性展平后还原为嵌套 Map。
     */
    public static Map<String, Object> exportFromEnvironment(Environment environment)
    {
        Map<String, Object> flat = new LinkedHashMap<>();
        if (environment instanceof ConfigurableEnvironment configurableEnvironment)
        {
            for (String key : collectPropertyKeys(configurableEnvironment))
            {
                if (shouldExport(key))
                {
                    String value = environment.getProperty(key);
                    if (value != null)
                    {
                        flat.put(key, coerceValue(value));
                    }
                }
            }
        }
        overlayResolvableInstallProperties(environment, flat);
        return unflattenProperties(flat);
    }

    /**
     * 覆盖枚举属性源未收录的环境变量（如 {@code SPRING_REDIS_HOST}），以 Spring 属性解析结果为准。
     */
    private static void overlayResolvableInstallProperties(Environment environment, Map<String, Object> flat)
    {
        for (String key : RESOLVABLE_INSTALL_PROPERTY_KEYS)
        {
            String value = environment.getProperty(key);
            if (value != null && !value.isBlank())
            {
                flat.put(key, coerceValue(value));
            }
        }
    }

    static boolean shouldExport(String key)
    {
        if (key == null || key.isBlank())
        {
            return false;
        }
        if (key.startsWith("cat2bug.install."))
        {
            return false;
        }
        if (key.startsWith("cat2bug.upgrade."))
        {
            return false;
        }
        if ("spring.profiles.active".equals(key))
        {
            return false;
        }
        if ("spring.database-type".equals(key))
        {
            return true;
        }
        if (key.startsWith("spring.datasource."))
        {
            return true;
        }
        if (key.startsWith("spring.redis."))
        {
            return true;
        }
        if (key.startsWith("spring.h2."))
        {
            return true;
        }
        if (key.startsWith("cat2bug.cache."))
        {
            return true;
        }
        if ("cat2bug.profile".equals(key) || "cat2bug.temp".equals(key))
        {
            return true;
        }
        if (key.startsWith("cat2bug.ai."))
        {
            return true;
        }
        if (key.startsWith("logging.file."))
        {
            return true;
        }
        return false;
    }

    static Map<String, Object> unflattenProperties(Map<String, Object> flat)
    {
        Map<String, Object> root = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : flat.entrySet())
        {
            putNested(root, entry.getKey(), entry.getValue());
        }
        return root;
    }

    private static Set<String> collectPropertyKeys(ConfigurableEnvironment environment)
    {
        Set<String> keys = new LinkedHashSet<>();
        for (PropertySource<?> propertySource : environment.getPropertySources())
        {
            if (propertySource instanceof EnumerablePropertySource<?> enumerable)
            {
                for (String name : enumerable.getPropertyNames())
                {
                    keys.add(name);
                }
            }
        }
        return keys;
    }

    @SuppressWarnings("unchecked")
    private static void putNested(Map<String, Object> root, String path, Object value)
    {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++)
        {
            current = (Map<String, Object>) current.computeIfAbsent(parts[i], key -> new LinkedHashMap<>());
        }
        current.put(parts[parts.length - 1], value);
    }

    private static Object coerceValue(String value)
    {
        String trimmed = value.trim();
        if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed))
        {
            return Boolean.parseBoolean(trimmed);
        }
        if (trimmed.matches("-?\\d+"))
        {
            try
            {
                return Integer.parseInt(trimmed);
            }
            catch (NumberFormatException ignored)
            {
                return value;
            }
        }
        return value;
    }
}
