package com.cat2bug.web.service.upgrade;

import com.cat2bug.common.config.InstallConfigExporter;
import com.cat2bug.common.config.InstallConfigMerge;
import com.cat2bug.common.config.InstallConfigSupport;
import com.cat2bug.common.config.InstallProperties;
import com.cat2bug.common.config.InstallTemplateLoader;
import com.cat2bug.common.config.UpgradeConfigMerger;
import com.cat2bug.framework.service.UpgradeService;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 升级预检：基于磁盘 install 与当前 JAR 模板对比配置 diff、待执行 Flyway 列表。
 */
@Service
public class UpgradePreflightService
{
    @Autowired
    private Environment environment;

    @Autowired
    private UpgradeService upgradeService;

    @Autowired
    private InstallProperties installProperties;

    @Autowired
    private UpgradeDatabaseBackupService upgradeDatabaseBackupService;

    public Map<String, Object> buildPreflight()
    {
        Map<String, Object> base = loadInstallBase();
        String databaseType = resolveDatabaseType(base);
        Map<String, Object> template = normalizeTemplateForInstall(
                InstallTemplateLoader.loadTemplate(databaseType), base, databaseType);
        Map<String, Object> merged = UpgradeConfigMerger.mergePreserveExisting(base, template, Map.of());

        Map<String, Object> status = upgradeService.getStatus();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("databaseType", databaseType);
        result.put("currentVersion", status.get("currentVersion"));
        result.put("targetVersion", status.get("targetVersion"));
        result.put("pendingMigrations", status.get("pendingMigrations"));
        result.put("diffs", buildDiffs(base, merged));
        result.put("suggestedConfig", merged);
        result.put("databaseName", upgradeDatabaseBackupService.resolveDatabaseName(buildSubmitFromBase(base, databaseType)));
        result.put("defaultBackupFileName", upgradeDatabaseBackupService.defaultBackupFileName(
                buildSubmitFromBase(base, databaseType)));
        result.put("backupDirectory", upgradeDatabaseBackupService.resolveBackupDirectory().toString());
        putFormFields(result, base, databaseType);
        return result;
    }

    private SetupSubmitRequest buildSubmitFromBase(Map<String, Object> base, String databaseType)
    {
        SetupSubmitRequest request = new SetupSubmitRequest();
        request.setDatabaseType(databaseType);
        if ("mysql".equalsIgnoreCase(databaseType))
        {
            Object master = nested(base, "spring", "datasource", "druid", "master");
            if (master instanceof Map<?, ?> masterMap)
            {
                request.setMysqlUsername(stringValue(masterMap.get("username")));
                request.setMysqlPassword(stringValue(masterMap.get("password")));
                parseMysqlJdbcUrl(stringValue(masterMap.get("url")), request);
            }
        }
        return request;
    }

    private static void parseMysqlJdbcUrl(String jdbcUrl, SetupSubmitRequest request)
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

    private void putFormFields(Map<String, Object> result, Map<String, Object> base, String databaseType)
    {
        result.put("cacheType", resolveCacheType(base));
        result.put("profile", stringAt(base, "cat2bug", "profile"));
        result.put("temp", stringAt(base, "cat2bug", "temp"));
        result.put("logPath", stringAt(base, "logging", "file", "path"));

        Object ai = nested(base, "cat2bug", "ai");
        if (ai instanceof Map<?, ?> aiMap)
        {
            result.put("aiEnabled", Boolean.TRUE.equals(aiMap.get("enabled")));
            result.put("aiHost", stringValue(aiMap.get("host")));
        }

        if ("mysql".equalsIgnoreCase(databaseType))
        {
            putMysqlFormFields(result, base);
        }

        if ("redis".equalsIgnoreCase(resolveCacheType(base)))
        {
            putRedisFormFields(result, base);
        }
    }

    private static void putMysqlFormFields(Map<String, Object> result, Map<String, Object> base)
    {
        Object master = nested(base, "spring", "datasource", "druid", "master");
        if (!(master instanceof Map<?, ?> masterMap))
        {
            return;
        }
        result.put("mysqlUsername", stringValue(masterMap.get("username")));
        result.put("mysqlPassword", stringValue(masterMap.get("password")));
        parseMysqlJdbcUrl(stringValue(masterMap.get("url")), result);
    }

    private static void putRedisFormFields(Map<String, Object> result, Map<String, Object> base)
    {
        Object redis = nested(base, "spring", "redis");
        if (!(redis instanceof Map<?, ?> redisMap))
        {
            return;
        }
        result.put("redisHost", stringValue(redisMap.get("host")));
        Object port = redisMap.get("port");
        if (port instanceof Number number)
        {
            result.put("redisPort", number.intValue());
        }
        result.put("redisPassword", stringValue(redisMap.get("password")));
        Object database = redisMap.get("database");
        if (database instanceof Number number)
        {
            result.put("redisDatabase", number.intValue());
        }
    }

    static void parseMysqlJdbcUrl(String jdbcUrl, Map<String, Object> target)
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

    private static String stringAt(Map<String, Object> root, String... path)
    {
        Object value = nested(root, path);
        return stringValue(value);
    }

    private static String stringValue(Object value)
    {
        return value == null ? "" : String.valueOf(value);
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

    /**
     * 优先读取磁盘 install 文件（含 {@code cat2bug.install.completed}）；无文件时回退 Environment 导出。
     */
    private Map<String, Object> loadInstallBase()
    {
        Map<String, Object> fromDisk = InstallConfigSupport.loadYamlMap(installProperties.resolveConfigPath());
        if (fromDisk != null && !fromDisk.isEmpty())
        {
            return fromDisk;
        }
        return InstallConfigExporter.exportFromEnvironment(environment);
    }

    private static String resolveDatabaseType(Map<String, Object> base)
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
        return "h2";
    }

    private static String resolveCacheType(Map<String, Object> base)
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
        return "local";
    }

    /**
     * 与安装向导一致：local 缓存不包含 Redis；H2 不含 MySQL 专属段。
     */
    private static Map<String, Object> normalizeTemplateForInstall(
            Map<String, Object> template,
            Map<String, Object> base,
            String databaseType)
    {
        Map<String, Object> normalized = InstallConfigMerge.deepCopy(template);
        if (!"redis".equalsIgnoreCase(resolveCacheType(base)))
        {
            removeRedisSection(normalized);
        }
        if ("h2".equalsIgnoreCase(databaseType))
        {
            removeRedisSection(normalized);
        }
        return normalized;
    }

    private List<Map<String, Object>> buildDiffs(Map<String, Object> base, Map<String, Object> merged)
    {
        List<Map<String, Object>> diffs = new ArrayList<>();
        collectDiffs("", base != null ? base : Map.of(), merged, diffs);
        return diffs;
    }

    @SuppressWarnings("unchecked")
    private void collectDiffs(
            String prefix,
            Map<String, Object> base,
            Map<String, Object> merged,
            List<Map<String, Object>> diffs)
    {
        for (Map.Entry<String, Object> entry : merged.entrySet())
        {
            String key = entry.getKey();
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            Object mergedValue = entry.getValue();
            Object baseValue = base.get(key);

            if (mergedValue instanceof Map<?, ?> mergedMap)
            {
                Map<String, Object> baseMap = baseValue instanceof Map<?, ?> bm
                        ? (Map<String, Object>) bm : Map.of();
                collectDiffs(path, baseMap, (Map<String, Object>) mergedMap, diffs);
                continue;
            }
            if (valuesEqual(baseValue, mergedValue))
            {
                continue;
            }
            boolean missing = isEmptyValue(baseValue);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", path);
            row.put("currentValue", baseValue);
            row.put("suggestedValue", mergedValue);
            row.put("missing", missing);
            row.put("action", missing ? "suggest" : "preserve");
            diffs.add(row);
        }
    }

    private static boolean valuesEqual(Object baseValue, Object mergedValue)
    {
        if (baseValue instanceof Boolean || mergedValue instanceof Boolean)
        {
            return Objects.equals(asBoolean(baseValue), asBoolean(mergedValue));
        }
        return Objects.equals(normalizeScalar(baseValue), normalizeScalar(mergedValue));
    }

    private static Boolean asBoolean(Object value)
    {
        if (value instanceof Boolean bool)
        {
            return bool;
        }
        if (value == null)
        {
            return null;
        }
        return Boolean.parseBoolean(String.valueOf(value).trim());
    }

    private static String normalizeScalar(Object value)
    {
        if (value == null)
        {
            return null;
        }
        return String.valueOf(value).trim();
    }

    private static boolean isEmptyValue(Object value)
    {
        if (value == null)
        {
            return true;
        }
        if (value instanceof String str)
        {
            return str.isBlank();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static void removeRedisSection(Map<String, Object> root)
    {
        Object spring = root.get("spring");
        if (spring instanceof Map<?, ?> springMap)
        {
            ((Map<String, Object>) springMap).remove("redis");
        }
    }
}
