package com.cat2bug.quarkus.common.config;

import java.util.Map;

/**
 * 从 install YAML 树读取数据源连接信息（Spring 模板格式）。
 */
public final class InstallRuntimeConfigLoader {

    private InstallRuntimeConfigLoader() {
    }

    public record DatabaseConnection(String driverClassName, String jdbcUrl, String username, String password) {
    }

    public static DatabaseConnection loadDatabaseConnection(String configPath) {
        Map<String, Object> root = InstallConfigSupport.loadYamlMap(InstallConfigSupport.resolveConfigPath(configPath));
        if (root == null) {
            return null;
        }
        String databaseType = readString(root, "spring", "database-type");
        if (databaseType == null || databaseType.isBlank()) {
            databaseType = "h2";
        }
        String url = readString(root, "spring", "datasource", "druid", "master", "url");
        if (url == null || url.isBlank()) {
            return null;
        }
        String username = readString(root, "spring", "datasource", "druid", "master", "username");
        String password = readString(root, "spring", "datasource", "druid", "master", "password");
        String driver = switch (databaseType.trim().toLowerCase()) {
            case "mysql" -> "com.mysql.cj.jdbc.Driver";
            default -> "org.h2.Driver";
        };
        if (username == null) {
            username = "mysql".equalsIgnoreCase(databaseType) ? "root" : "root";
        }
        if (password == null) {
            password = "mysql".equalsIgnoreCase(databaseType) ? "" : "cat2bug_password";
        }
        return new DatabaseConnection(driver, url, username, password);
    }

    public static String readCacheType(String configPath) {
        Map<String, Object> root = InstallConfigSupport.loadYamlMap(InstallConfigSupport.resolveConfigPath(configPath));
        if (root == null) {
            return "local";
        }
        String cacheType = readString(root, "cat2bug", "cache", "type");
        return cacheType != null && !cacheType.isBlank() ? cacheType.trim().toLowerCase() : "local";
    }

    public static String readProfile(String configPath) {
        Map<String, Object> root = InstallConfigSupport.loadYamlMap(InstallConfigSupport.resolveConfigPath(configPath));
        if (root == null) {
            return null;
        }
        return readString(root, "cat2bug", "profile");
    }

    public static boolean readAiEnabled(String configPath) {
        Map<String, Object> root = InstallConfigSupport.loadYamlMap(InstallConfigSupport.resolveConfigPath(configPath));
        if (root == null) {
            return false;
        }
        Object value = readObject(root, "cat2bug", "ai", "enabled");
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    public static String readAiHost(String configPath) {
        Map<String, Object> root = InstallConfigSupport.loadYamlMap(InstallConfigSupport.resolveConfigPath(configPath));
        if (root == null) {
            return null;
        }
        return readString(root, "cat2bug", "ai", "host");
    }

    @SuppressWarnings("unchecked")
    private static Object readObject(Map<String, Object> root, String... path) {
        Object current = root;
        for (String key : path) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = map.get(key);
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private static String readString(Map<String, Object> root, String... path) {
        Object current = readObject(root, path);
        if (current == null) {
            return null;
        }
        String value = String.valueOf(current).trim();
        return "null".equalsIgnoreCase(value) ? null : value;
    }
}
