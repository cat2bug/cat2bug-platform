package com.cat2bug.web.service.setup;

import com.cat2bug.web.domain.setup.SetupSubmitRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 安装/升级向导表单字段与 install 配置树之间的解析（JDBC URL、Redis、AI、存储路径等）。
 */
public final class SetupFormDefaultsSupport
{
    private SetupFormDefaultsSupport()
    {
    }

    public static void putFormFields(
            Map<String, Object> result,
            Map<String, Object> base,
            String databaseType,
            String cacheType)
    {
        putOptionalString(result, "profile", stringAt(base, "cat2bug", "profile"));
        putOptionalString(result, "temp", stringAt(base, "cat2bug", "temp"));
        putOptionalString(result, "logPath", stringAt(base, "logging", "file", "path"));

        Object ai = nested(base, "cat2bug", "ai");
        if (ai instanceof Map<?, ?> aiMap)
        {
            if (aiMap.containsKey("enabled"))
            {
                result.put("aiEnabled", Boolean.TRUE.equals(aiMap.get("enabled")));
            }
            putOptionalString(result, "aiHost", stringValue(aiMap.get("host")));
        }

        if ("mysql".equalsIgnoreCase(databaseType))
        {
            putMysqlFormFields(result, base);
        }

        if ("redis".equalsIgnoreCase(cacheType))
        {
            putRedisFormFields(result, base);
        }
    }

    public static void putMysqlFormFields(Map<String, Object> result, Map<String, Object> base)
    {
        Object master = nested(base, "spring", "datasource", "druid", "master");
        if (!(master instanceof Map<?, ?> masterMap))
        {
            return;
        }
        putOptionalString(result, "mysqlUsername", stringValue(masterMap.get("username")));
        putOptionalString(result, "mysqlPassword", stringValue(masterMap.get("password")));
        parseMysqlJdbcUrl(stringValue(masterMap.get("url")), result);
    }

    public static void putRedisFormFields(Map<String, Object> result, Map<String, Object> base)
    {
        Object redis = nested(base, "spring", "redis");
        if (!(redis instanceof Map<?, ?> redisMap))
        {
            return;
        }
        putOptionalString(result, "redisHost", stringValue(redisMap.get("host")));
        Object port = redisMap.get("port");
        if (port instanceof Number number)
        {
            result.put("redisPort", number.intValue());
        }
        putOptionalString(result, "redisPassword", stringValue(redisMap.get("password")));
        Object database = redisMap.get("database");
        if (database instanceof Number number)
        {
            result.put("redisDatabase", number.intValue());
        }
    }

    public static void parseMysqlJdbcUrl(String jdbcUrl, SetupSubmitRequest request)
    {
        Map<String, Object> parsed = new LinkedHashMap<>();
        parseMysqlJdbcUrl(jdbcUrl, parsed);
        Object host = parsed.get("mysqlHost");
        if (host != null)
        {
            request.setMysqlHost(String.valueOf(host));
        }
        Object port = parsed.get("mysqlPort");
        if (port instanceof Number number)
        {
            request.setMysqlPort(number.intValue());
        }
        Object database = parsed.get("mysqlDatabase");
        if (database != null)
        {
            request.setMysqlDatabase(String.valueOf(database));
        }
    }

    public static void parseMysqlJdbcUrl(String jdbcUrl, Map<String, Object> target)
    {
        if (jdbcUrl == null || jdbcUrl.isBlank())
        {
            return;
        }
        int schemeEnd = jdbcUrl.indexOf("://");
        if (schemeEnd < 0)
        {
            return;
        }
        String remainder = jdbcUrl.substring(schemeEnd + 3);
        int slash = remainder.indexOf('/');
        String authority = slash >= 0 ? remainder.substring(0, slash) : remainder;
        String path = slash >= 0 ? remainder.substring(slash + 1) : "";
        int query = path.indexOf('?');
        if (query >= 0)
        {
            path = path.substring(0, query);
        }
        int colon = authority.indexOf(':');
        if (colon >= 0)
        {
            target.put("mysqlHost", authority.substring(0, colon));
            target.put("mysqlPort", Integer.parseInt(authority.substring(colon + 1)));
        }
        else
        {
            target.put("mysqlHost", authority);
            target.put("mysqlPort", 3306);
        }
        if (!path.isBlank())
        {
            target.put("mysqlDatabase", path);
        }
    }

    /** 升级预检：显式 database-type，否则默认 h2 */
    public static String resolveDatabaseType(Map<String, Object> base)
    {
        String explicit = readDatabaseType(base);
        return explicit != null ? explicit : "h2";
    }

    /**
     * 安装预检：显式 database-type，或 JDBC URL 含 jdbc:mysql；否则不返回（由前端保留默认）。
     */
    public static String resolveInferredDatabaseType(Map<String, Object> base)
    {
        String jdbcUrl = stringValue(nested(base, "spring", "datasource", "druid", "master", "url"));
        if (jdbcUrl.contains("jdbc:mysql"))
        {
            return "mysql";
        }
        String explicit = readDatabaseType(base);
        if (explicit != null)
        {
            return explicit;
        }
        return null;
    }

    /** 升级预检：显式 cache.type，否则默认 local */
    public static String resolveCacheType(Map<String, Object> base)
    {
        String explicit = readCacheType(base);
        return explicit != null ? explicit : "local";
    }

    /**
     * 安装预检：显式 cache.type，或 spring.redis.host 非空；否则不返回。
     */
    public static String resolveInferredCacheType(Map<String, Object> base)
    {
        String explicit = readCacheType(base);
        if (explicit != null)
        {
            return explicit;
        }
        String redisHost = stringValue(nested(base, "spring", "redis", "host"));
        if (!redisHost.isBlank())
        {
            return "redis";
        }
        return null;
    }

    private static String readDatabaseType(Map<String, Object> base)
    {
        Object spring = base.get("spring");
        if (spring instanceof Map<?, ?> springMap)
        {
            Object databaseType = springMap.get("database-type");
            if (databaseType != null && !String.valueOf(databaseType).isBlank())
            {
                return String.valueOf(databaseType).trim().toLowerCase();
            }
        }
        return null;
    }

    private static String readCacheType(Map<String, Object> base)
    {
        Object cat2bug = base.get("cat2bug");
        if (cat2bug instanceof Map<?, ?> cat2bugMap)
        {
            Object cache = cat2bugMap.get("cache");
            if (cache instanceof Map<?, ?> cacheMap)
            {
                Object type = cacheMap.get("type");
                if (type != null && !String.valueOf(type).isBlank())
                {
                    return String.valueOf(type).trim().toLowerCase();
                }
            }
        }
        return null;
    }

    private static void putOptionalString(Map<String, Object> result, String key, String value)
    {
        if (value != null && !value.isBlank())
        {
            result.put(key, value);
        }
    }

    private static String stringAt(Map<String, Object> root, String... path)
    {
        return stringValue(nested(root, path));
    }

    public static String stringValue(Object value)
    {
        return value == null ? "" : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    public static Object nested(Map<String, Object> root, String... path)
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
