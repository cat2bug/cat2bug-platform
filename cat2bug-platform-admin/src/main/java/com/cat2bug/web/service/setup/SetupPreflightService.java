package com.cat2bug.web.service.setup;

import com.cat2bug.common.config.InstallConfigExporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 首次安装向导预检：从 Spring Environment 导出表单默认值。
 */
@Service
public class SetupPreflightService
{
    @Autowired
    private Environment environment;

    public Map<String, Object> buildPreflight()
    {
        Map<String, Object> base = InstallConfigExporter.exportFromEnvironment(environment);
        if (base == null)
        {
            base = new LinkedHashMap<>();
        }

        String databaseType = SetupFormDefaultsSupport.resolveInferredDatabaseType(base);
        String cacheType = SetupFormDefaultsSupport.resolveInferredCacheType(base);

        Map<String, Object> result = new LinkedHashMap<>();
        SetupDockerPreflightSupport.applyDefaults(result, environment, databaseType, cacheType);
        if (databaseType != null)
        {
            result.putIfAbsent("databaseType", databaseType);
        }
        if (cacheType != null)
        {
            result.putIfAbsent("cacheType", cacheType);
        }

        String effectiveDatabaseType = stringValue(result.get("databaseType"));
        if (effectiveDatabaseType.isBlank())
        {
            effectiveDatabaseType = databaseType != null
                    ? databaseType
                    : SetupFormDefaultsSupport.resolveDatabaseType(base);
        }
        String effectiveCacheType = stringValue(result.get("cacheType"));
        if (effectiveCacheType.isBlank())
        {
            effectiveCacheType = cacheType != null
                    ? cacheType
                    : SetupFormDefaultsSupport.resolveCacheType(base);
        }
        SetupFormDefaultsSupport.putFormFields(result, base, effectiveDatabaseType, effectiveCacheType);
        SetupDockerPreflightSupport.applyHostDefaults(result, environment);
        return result;
    }

    private static String stringValue(Object value)
    {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
