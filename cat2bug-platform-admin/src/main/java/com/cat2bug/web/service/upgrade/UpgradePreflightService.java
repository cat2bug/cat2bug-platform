package com.cat2bug.web.service.upgrade;

import com.cat2bug.common.config.InstallConfigExporter;
import com.cat2bug.common.config.InstallConfigSupport;
import com.cat2bug.common.config.InstallProperties;
import com.cat2bug.common.config.InstallTemplateLoader;
import com.cat2bug.common.config.UpgradeConfigMerger;
import com.cat2bug.common.config.UpgradeSupport;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.framework.service.UpgradeService;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import com.cat2bug.web.service.setup.SetupConfigWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 升级预检：导出配置 diff、待执行 Flyway 列表。
 */
@Service
public class UpgradePreflightService
{
    @Autowired
    private Environment environment;

    @Autowired
    private UpgradeService upgradeService;

    @Autowired
    private SetupConfigWriter setupConfigWriter;

    public Map<String, Object> buildPreflight()
    {
        String databaseType = environment.getProperty("spring.database-type", "h2");
        Map<String, Object> base = InstallConfigExporter.exportFromEnvironment(environment);
        Map<String, Object> template = InstallTemplateLoader.loadTemplate(databaseType);
        Map<String, Object> merged = UpgradeConfigMerger.mergePreserveExisting(base, template, Map.of());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("databaseType", databaseType);
        result.put("currentVersion", upgradeService.getStatus().get("currentVersion"));
        result.put("targetVersion", upgradeService.getStatus().get("targetVersion"));
        result.put("pendingMigrations", upgradeService.listPendingMigrations());
        result.put("diffs", buildDiffs(base, template, merged));
        result.put("suggestedConfig", merged);
        return result;
    }

    private List<Map<String, Object>> buildDiffs(
            Map<String, Object> base,
            Map<String, Object> template,
            Map<String, Object> merged)
    {
        List<Map<String, Object>> diffs = new ArrayList<>();
        collectDiffs("", base, template, merged, diffs);
        return diffs;
    }

    @SuppressWarnings("unchecked")
    private void collectDiffs(
            String prefix,
            Map<String, Object> base,
            Map<String, Object> template,
            Map<String, Object> merged,
            List<Map<String, Object>> diffs)
    {
        for (Map.Entry<String, Object> entry : template.entrySet())
        {
            String key = entry.getKey();
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            Object templateValue = entry.getValue();
            Object baseValue = base != null ? base.get(key) : null;
            Object mergedValue = merged.get(key);

            if (templateValue instanceof Map<?, ?> templateMap)
            {
                Map<String, Object> baseMap = baseValue instanceof Map<?, ?> bm
                        ? (Map<String, Object>) bm : Map.of();
                Map<String, Object> mergedMap = mergedValue instanceof Map<?, ?> mm
                        ? (Map<String, Object>) mm : Map.of();
                collectDiffs(path, baseMap, (Map<String, Object>) templateMap, mergedMap, diffs);
            }
            else
            {
                boolean missing = baseValue == null || (baseValue instanceof String s && s.isBlank());
                if (missing || !String.valueOf(baseValue).equals(String.valueOf(templateValue)))
                {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("key", path);
                    row.put("currentValue", baseValue);
                    row.put("suggestedValue", mergedValue);
                    row.put("missing", missing);
                    row.put("action", missing ? "suggest" : "preserve");
                    diffs.add(row);
                }
            }
        }
    }
}
