package com.cat2bug.common.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 从 classpath 加载安装配置模板（defaults/application-install-*.yml）。
 */
public final class InstallTemplateLoader {

    public static final String H2_TEMPLATE_PATH = "defaults/application-install-h2.yml";

    public static final String MYSQL_TEMPLATE_PATH = "defaults/application-install-mysql.yml";

    private InstallTemplateLoader() {
    }

    public static Map<String, Object> loadH2Template() {
        return loadClasspathTemplate(H2_TEMPLATE_PATH);
    }

    public static Map<String, Object> loadMysqlTemplate() {
        return loadClasspathTemplate(MYSQL_TEMPLATE_PATH);
    }

    public static Map<String, Object> loadTemplate(String databaseType) {
        if (databaseType == null || databaseType.isBlank()) {
            throw new IllegalArgumentException("databaseType must not be blank");
        }
        String normalized = databaseType.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "h2" -> loadH2Template();
            case "mysql" -> loadMysqlTemplate();
            default -> throw new IllegalArgumentException("Unsupported database type: " + databaseType);
        };
    }

    private static Map<String, Object> loadClasspathTemplate(String classpathPath) {
        ClassLoader classLoader = InstallTemplateLoader.class.getClassLoader();
        try (InputStream in = classLoader.getResourceAsStream(classpathPath)) {
            if (in == null) {
                throw new IllegalStateException("Install template not found on classpath: " + classpathPath);
            }
            Object loaded = new Yaml().load(in);
            if (!(loaded instanceof Map)) {
                throw new IllegalStateException("Install template must be a YAML mapping: " + classpathPath);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> root = (Map<String, Object>) loaded;
            return root;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load install template: " + classpathPath, ex);
        }
    }
}
