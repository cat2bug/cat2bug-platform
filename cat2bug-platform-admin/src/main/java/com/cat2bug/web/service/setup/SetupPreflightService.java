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
        SetupFormDefaultsSupport.putFormFields(result, base,
                databaseType != null ? databaseType : SetupFormDefaultsSupport.resolveDatabaseType(base),
                cacheType != null ? cacheType : SetupFormDefaultsSupport.resolveCacheType(base));

        if (databaseType != null)
        {
            result.putIfAbsent("databaseType", databaseType);
        }
        if (cacheType != null)
        {
            result.putIfAbsent("cacheType", cacheType);
        }

        SetupDockerPreflightSupport.applyDefaults(result, environment, databaseType, cacheType);

        // compose / K8s 环境变量最后写入，覆盖引导期 H2 与前端默认 127.0.0.1
        SetupEnvironmentPreflightSupport.applyExplicitConfiguration(result, environment);

        return result;
    }
}
