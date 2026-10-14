package com.cat2bug.quarkus.admin.setup;

import com.cat2bug.quarkus.common.config.InstallConfigMerge;
import com.cat2bug.quarkus.common.util.StringUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 从 Quarkus 运行期配置导出安装相关配置树，供升级预检在无 install 文件时回退使用。
 */
@ApplicationScoped
public class QuarkusInstallConfigExporter {

    @ConfigProperty(name = "quarkus.mybatis.database-id", defaultValue = "h2")
    String databaseId;

    @ConfigProperty(name = "quarkus.datasource.runtime-mysql.jdbc.url")
    Optional<String> runtimeMysqlJdbcUrl;

    @ConfigProperty(name = "quarkus.datasource.runtime-h2.jdbc.url")
    Optional<String> runtimeH2JdbcUrl;

    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    Optional<String> defaultJdbcUrl;

    @ConfigProperty(name = "quarkus.datasource.runtime-mysql.username", defaultValue = "root")
    String runtimeMysqlUsername;

    @ConfigProperty(name = "quarkus.datasource.runtime-h2.username", defaultValue = "root")
    String runtimeH2Username;

    @ConfigProperty(name = "quarkus.datasource.username", defaultValue = "sa")
    String defaultUsername;

    @ConfigProperty(name = "quarkus.datasource.runtime-mysql.password", defaultValue = "")
    Optional<String> runtimeMysqlPassword;

    @ConfigProperty(name = "quarkus.datasource.runtime-h2.password", defaultValue = "")
    Optional<String> runtimeH2Password;

    @ConfigProperty(name = "quarkus.datasource.password", defaultValue = "")
    Optional<String> defaultPassword;

    @ConfigProperty(name = "quarkus.mybatis.datasource", defaultValue = "")
    Optional<String> mybatisDatasource;

    @ConfigProperty(name = "cat2bug.cache.type", defaultValue = "local")
    String cacheType;

    @ConfigProperty(name = "cat2bug.profile", defaultValue = "./upload")
    String profile;

    @ConfigProperty(name = "cat2bug.temp", defaultValue = "./temp")
    String temp;

    @ConfigProperty(name = "quarkus.log.file.path", defaultValue = "./logs")
    String logPath;

    public Map<String, Object> exportFromRuntime() {
        String databaseType = "mysql".equalsIgnoreCase(databaseId) ? "mysql" : "h2";
        Map<String, Object> root = new LinkedHashMap<>();

        Map<String, Object> spring = new LinkedHashMap<>();
        spring.put("database-type", databaseType);

        Map<String, Object> datasource = new LinkedHashMap<>();
        Map<String, Object> druid = new LinkedHashMap<>();
        Map<String, Object> master = new LinkedHashMap<>();
        resolveJdbcUrl().ifPresent(url -> master.put("url", url));
        master.put("username", resolveUsername());
        master.put("password", resolvePassword().orElse(""));
        druid.put("master", master);
        datasource.put("druid", druid);
        spring.put("datasource", datasource);
        root.put("spring", spring);

        Map<String, Object> cat2bug = new LinkedHashMap<>();
        cat2bug.put("profile", profile);
        cat2bug.put("temp", temp);
        Map<String, Object> cache = new LinkedHashMap<>();
        cache.put("type", StringUtils.isNotEmpty(cacheType) ? cacheType.toLowerCase() : "local");
        cat2bug.put("cache", cache);
        root.put("cat2bug", cat2bug);

        Map<String, Object> logging = new LinkedHashMap<>();
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("path", logPath);
        logging.put("file", file);
        root.put("logging", logging);

        return InstallConfigMerge.deepCopy(root);
    }

    private Optional<String> resolveJdbcUrl() {
        String ds = mybatisDatasource.orElse("");
        if (InstallDatasourceNames.RUNTIME_MYSQL.equals(ds)) {
            return runtimeMysqlJdbcUrl;
        }
        if (InstallDatasourceNames.RUNTIME_H2.equals(ds)) {
            return runtimeH2JdbcUrl;
        }
        return defaultJdbcUrl;
    }

    private String resolveUsername() {
        String ds = mybatisDatasource.orElse("");
        if (InstallDatasourceNames.RUNTIME_MYSQL.equals(ds)) {
            return runtimeMysqlUsername;
        }
        if (InstallDatasourceNames.RUNTIME_H2.equals(ds)) {
            return runtimeH2Username;
        }
        return defaultUsername;
    }

    private Optional<String> resolvePassword() {
        String ds = mybatisDatasource.orElse("");
        if (InstallDatasourceNames.RUNTIME_MYSQL.equals(ds)) {
            return runtimeMysqlPassword;
        }
        if (InstallDatasourceNames.RUNTIME_H2.equals(ds)) {
            return runtimeH2Password;
        }
        return defaultPassword;
    }
}
