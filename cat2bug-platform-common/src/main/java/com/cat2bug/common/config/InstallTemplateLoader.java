package com.cat2bug.common.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 从 classpath 加载安装配置模板（{@code defaults/application-install-*.yml}），
 * 供引导期 EnvironmentPostProcessor、安装向导写入与 Legacy 迁移使用。
 * <p>
 * 模板资源由 {@code cat2bug-platform-admin} 模块提供，运行时须在同一 ClassLoader 下可见。
 * </p>
 */
public final class InstallTemplateLoader
{
    public static final String H2_TEMPLATE_PATH = "defaults/application-install-h2.yml";

    public static final String MYSQL_TEMPLATE_PATH = "defaults/application-install-mysql.yml";

    private InstallTemplateLoader()
    {
    }

    /**
     * 加载 H2 安装配置模板。
     */
    public static Map<String, Object> loadH2Template()
    {
        return loadClasspathTemplate(H2_TEMPLATE_PATH);
    }

    /**
     * 加载 MySQL 安装配置模板。
     */
    public static Map<String, Object> loadMysqlTemplate()
    {
        return loadClasspathTemplate(MYSQL_TEMPLATE_PATH);
    }

    /**
     * 按数据库类型加载模板。
     *
     * @param databaseType {@code h2} 或 {@code mysql}（大小写不敏感）
     */
    public static Map<String, Object> loadTemplate(String databaseType)
    {
        if (databaseType == null || databaseType.isBlank())
        {
            throw new IllegalArgumentException("databaseType must not be blank");
        }
        String normalized = databaseType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized)
        {
            case "h2" -> loadH2Template();
            case "mysql" -> loadMysqlTemplate();
            default -> throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        };
    }

    /**
     * 将嵌套 YAML Map 展平为 Spring {@code Environment} 可用的点分属性键。
     * 例如 {@code spring.datasource.druid.master.url}。
     */
    public static Map<String, Object> flattenToProperties(Map<String, Object> source)
    {
        Map<String, Object> flat = new LinkedHashMap<>();
        if (source != null)
        {
            flattenMap("", source, flat);
        }
        return flat;
    }

    private static Map<String, Object> loadClasspathTemplate(String classpathPath)
    {
        ClassLoader classLoader = InstallTemplateLoader.class.getClassLoader();
        try (InputStream in = classLoader.getResourceAsStream(classpathPath))
        {
            if (in == null)
            {
                throw new IllegalStateException("Install template not found on classpath: " + classpathPath);
            }
            Object loaded = new Yaml().load(in);
            if (!(loaded instanceof Map))
            {
                throw new IllegalStateException("Install template must be a YAML mapping: " + classpathPath);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> root = (Map<String, Object>) loaded;
            return root;
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("Failed to load install template: " + classpathPath, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static void flattenMap(String prefix, Map<String, Object> source, Map<String, Object> target)
    {
        for (Map.Entry<String, Object> entry : source.entrySet())
        {
            String key = entry.getKey();
            Object value = entry.getValue();
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (value instanceof Map)
            {
                flattenMap(fullKey, (Map<String, Object>) value, target);
            }
            else
            {
                target.put(fullKey, value);
            }
        }
    }
}
