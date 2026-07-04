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
        if (base == null || base.isEmpty())
        {
            return Map.of();
        }

        String databaseType = SetupFormDefaultsSupport.resolveInferredDatabaseType(base);
        String cacheType = SetupFormDefaultsSupport.resolveInferredCacheType(base);

        Map<String, Object> result = new LinkedHashMap<>();
        if (databaseType != null)
        {
            result.put("databaseType", databaseType);
        }
        if (cacheType != null)
        {
            result.put("cacheType", cacheType);
        }

        String effectiveDatabaseType = databaseType != null
                ? databaseType
                : SetupFormDefaultsSupport.resolveDatabaseType(base);
        String effectiveCacheType = cacheType != null
                ? cacheType
                : SetupFormDefaultsSupport.resolveCacheType(base);
        SetupFormDefaultsSupport.putFormFields(result, base, effectiveDatabaseType, effectiveCacheType);
        return result;
    }
}
