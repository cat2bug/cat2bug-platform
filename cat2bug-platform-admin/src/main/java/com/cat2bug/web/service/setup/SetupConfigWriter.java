package com.cat2bug.web.service.setup;

import com.cat2bug.common.config.InstallProperties;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.web.domain.setup.SetupDatabaseTestRequest;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将安装选项写入 config/install/application-install.yml。
 * <p>
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
        Files.createDirectories(target.getParent());
        Map<String, Object> root = buildYamlRoot(request);
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        String content = yaml.dump(root);
        Files.writeString(target, content, StandardCharsets.UTF_8);
        return target;
    }

    private Map<String, Object> buildYamlRoot(SetupSubmitRequest request)
    {
        Map<String, Object> root = new LinkedHashMap<>();
        String databaseType = request.getDatabaseType().toLowerCase();
        String cacheType = StringUtils.isNotEmpty(request.getCacheType())
                ? request.getCacheType().toLowerCase() : "local";

        Map<String, Object> spring = new LinkedHashMap<>();
        spring.put("database-type", databaseType);

        Map<String, Object> datasource = new LinkedHashMap<>();
        Map<String, Object> druid = new LinkedHashMap<>();
        Map<String, Object> master = new LinkedHashMap<>();
        if ("h2".equals(databaseType))
        {
            datasource.put("type", "com.alibaba.druid.pool.DruidDataSource");
            datasource.put("driverClassName", "org.h2.Driver");
            master.put("url", "jdbc:h2:file:./data/cat2bug;MODE=MySQL;DATABASE_TO_LOWER=TRUE;");
            master.put("username", "root");
            master.put("password", "cat2bug_password");
        }
        else
        {
            master.put("url", SetupDatabaseTestService.resolveMysqlUrl(toDatabaseTestRequest(request)));
            master.put("username", request.getMysqlUsername());
            master.put("password", request.getMysqlPassword());
        }
        druid.put("master", master);
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
        root.put("spring", spring);

        Map<String, Object> cat2bug = new LinkedHashMap<>();
        Map<String, Object> cache = new LinkedHashMap<>();
        cache.put("type", cacheType);
        cat2bug.put("cache", cache);
        // 与 Docker 卷对齐时，保持容器内路径与挂载点一致
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
        root.put("cat2bug", cat2bug);

        if (StringUtils.isNotEmpty(request.getLogPath()))
        {
            Map<String, Object> logging = new LinkedHashMap<>();
            Map<String, Object> file = new LinkedHashMap<>();
            file.put("path", request.getLogPath());
            logging.put("file", file);
            root.put("logging", logging);
        }
        return root;
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
