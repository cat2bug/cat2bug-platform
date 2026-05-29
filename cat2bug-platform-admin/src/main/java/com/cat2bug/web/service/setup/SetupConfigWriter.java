package com.cat2bug.web.service.setup;

import com.cat2bug.common.config.InstallConfigMerge;
import com.cat2bug.common.config.InstallConfigSupport;
import com.cat2bug.common.config.InstallProperties;
import com.cat2bug.common.config.InstallTemplateLoader;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.web.domain.setup.SetupDatabaseTestRequest;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将安装选项写入 config/install/application-install.yml。
 * <p>
 * 基于 classpath 模板合并向导输入，写入完整基础设施配置并标记 {@code cat2bug.install.completed: true}。
 * cat2bug.profile / cat2bug.temp：文件存储与临时目录，Docker 部署时建议挂载卷到相同路径。
 * </p>
 */
@Service
public class SetupConfigWriter
{
    @Autowired
    private InstallProperties installProperties;

    public Path write(SetupSubmitRequest request) throws IOException
    {
        Path target = Paths.get(installProperties.getConfigPath()).toAbsolutePath().normalize();
        Map<String, Object> root = buildYamlRoot(request);
        InstallConfigSupport.writeInstallConfig(target, root);
        return target;
    }

    public Map<String, Object> buildYamlRoot(SetupSubmitRequest request)
    {
        String databaseType = request.getDatabaseType().toLowerCase();
        String cacheType = StringUtils.isNotEmpty(request.getCacheType())
                ? request.getCacheType().toLowerCase() : "local";

        Map<String, Object> root = InstallConfigMerge.deepCopy(InstallTemplateLoader.loadTemplate(databaseType));
        InstallConfigMerge.deepMerge(root, buildOverrides(request, databaseType, cacheType));

        if ("local".equals(cacheType))
        {
            removeRedisSection(root);
        }
        else if ("redis".equals(cacheType) && "h2".equals(databaseType))
        {
            mergeH2RedisDefaults(root);
        }

        InstallConfigSupport.setInstallCompleted(root, true);
        return root;
    }

    private Map<String, Object> buildOverrides(SetupSubmitRequest request, String databaseType, String cacheType)
    {
        Map<String, Object> overrides = new LinkedHashMap<>();

        Map<String, Object> spring = new LinkedHashMap<>();
        spring.put("database-type", databaseType);

        Map<String, Object> master = new LinkedHashMap<>();
        if ("mysql".equals(databaseType))
        {
            master.put("url", SetupDatabaseTestService.resolveMysqlUrl(toDatabaseTestRequest(request)));
            master.put("username", request.getMysqlUsername());
            master.put("password", request.getMysqlPassword());
        }
        Map<String, Object> druid = new LinkedHashMap<>();
        druid.put("master", master);
        Map<String, Object> datasource = new LinkedHashMap<>();
        datasource.put("druid", druid);
        spring.put("datasource", datasource);

        if ("redis".equals(cacheType))
        {
            Map<String, Object> redis = new LinkedHashMap<>();
            redis.put("host", request.getRedisHost());
            redis.put("port", request.getRedisPort() != null ? request.getRedisPort() : 6379);
            redis.put("database", request.getRedisDatabase() != null ? request.getRedisDatabase() : 0);
            if (StringUtils.isNotEmpty(request.getRedisPassword()))
            {
                redis.put("password", request.getRedisPassword());
            }
            spring.put("redis", redis);
        }
        overrides.put("spring", spring);

        Map<String, Object> cat2bug = new LinkedHashMap<>();
        Map<String, Object> cache = new LinkedHashMap<>();
        cache.put("type", cacheType);
        cat2bug.put("cache", cache);
        cat2bug.put("profile", request.getProfile());
        cat2bug.put("temp", request.getTemp());

        Map<String, Object> ai = new LinkedHashMap<>();
        ai.put("enabled", Boolean.TRUE.equals(request.getAiEnabled()));
        if (StringUtils.isNotEmpty(request.getAiHost()))
        {
            ai.put("host", request.getAiHost());
        }
        if (StringUtils.isNotEmpty(request.getAiDefaultModel()))
        {
            ai.put("default-business-model", request.getAiDefaultModel());
        }
        cat2bug.put("ai", ai);
        overrides.put("cat2bug", cat2bug);

        if (StringUtils.isNotEmpty(request.getLogPath()))
        {
            Map<String, Object> logging = new LinkedHashMap<>();
            Map<String, Object> file = new LinkedHashMap<>();
            file.put("path", request.getLogPath());
            logging.put("file", file);
            overrides.put("logging", logging);
        }
        return overrides;
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

    @SuppressWarnings("unchecked")
    private static void mergeH2RedisDefaults(Map<String, Object> root)
    {
        Object mysqlSpring = InstallTemplateLoader.loadMysqlTemplate().get("spring");
        if (mysqlSpring instanceof Map<?, ?> mysqlSpringMap)
        {
            Object redis = mysqlSpringMap.get("redis");
            if (redis instanceof Map<?, ?> redisMap)
            {
                Map<String, Object> spring = (Map<String, Object>) root.computeIfAbsent("spring", key -> new LinkedHashMap<>());
                if (!spring.containsKey("redis"))
                {
                    spring.put("redis", InstallConfigMerge.deepCopy((Map<String, Object>) redisMap));
                }
            }
        }
    }

    private static SetupDatabaseTestRequest toDatabaseTestRequest(SetupSubmitRequest request)
    {
        SetupDatabaseTestRequest test = new SetupDatabaseTestRequest();
        test.setDatabaseType("mysql");
        test.setHost(request.getMysqlHost());
        test.setPort(request.getMysqlPort());
        test.setDatabase(request.getMysqlDatabase());
        test.setUsername(request.getMysqlUsername());
        test.setPassword(request.getMysqlPassword());
        return test;
    }
}
