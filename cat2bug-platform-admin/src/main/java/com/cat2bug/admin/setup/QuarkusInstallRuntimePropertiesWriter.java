package com.cat2bug.admin.setup;

import com.cat2bug.common.config.InstallConfigSupport;
import com.cat2bug.common.config.InstallRuntimeConfigLoader;
import com.cat2bug.common.domain.setup.SetupSubmitRequest;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.mybatis.QuarkusMapperDataSources;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 将 install 向导选项映射为 Quarkus 可加载的 {@code quarkus-runtime.properties}，
 * 供重启后切换数据源并退出引导模式。
 */
@ApplicationScoped
public class QuarkusInstallRuntimePropertiesWriter {

    @ConfigProperty(name = "cat2bug.install.config-path", defaultValue = InstallConfigSupport.DEFAULT_CONFIG_PATH)
    String configPath;

    public static final String PENDING_RESTART_MARKER = ".cat2bug-pending-restart";

    public Path runtimePropertiesPath() {
        Path installPath = InstallConfigSupport.resolveWorkingDirectoryConfigPath(configPath);
        Path parent = installPath.getParent();
        if (parent == null) {
            return Path.of("config", "install", "quarkus-runtime.properties").toAbsolutePath().normalize();
        }
        return parent.resolve("quarkus-runtime.properties").toAbsolutePath().normalize();
    }

    public Path pendingRestartMarkerPath() {
        return runtimePropertiesPath().getParent().resolve(PENDING_RESTART_MARKER).toAbsolutePath().normalize();
    }

    public void clearPendingRestartMarker() {
        try {
            Files.deleteIfExists(pendingRestartMarkerPath());
        } catch (IOException ignored) {
            // 非关键路径
        }
    }

    public Path writeFromSubmit(SetupSubmitRequest request) throws IOException {
        return writeLines(buildLinesFromSubmit(request));
    }

    public Path writeFromInstallFile() throws IOException {
        InstallRuntimeConfigLoader.DatabaseConnection connection =
                InstallRuntimeConfigLoader.loadDatabaseConnection(configPath);
        if (connection == null) {
            return null;
        }
        String cacheType = InstallRuntimeConfigLoader.readCacheType(configPath);
        List<String> lines = new ArrayList<>();
        lines.add("cat2bug.bootstrap.mode=false");
        lines.add("cat2bug.cache.type=" + cacheType);
        String profile = InstallRuntimeConfigLoader.readProfile(configPath);
        if (StringUtils.isNotEmpty(profile)) {
            lines.add("cat2bug.profile=" + profile);
        }
        appendDatabaseLines(lines, connection);
        appendFlywayLines(lines);
        appendAiLines(lines, configPath);
        return writeLines(lines);
    }

    /** 安装完成后的运行期 Flyway：仅对齐元数据表名，不在启动时重复 migrate（与 Spring flyway.enabled=false 一致）。 */
    private static void appendFlywayLines(List<String> lines) {
        lines.add("quarkus.flyway.migrate-at-start=false");
        lines.add("quarkus.flyway.table=sys_db_version");
    }

    private List<String> buildLinesFromSubmit(SetupSubmitRequest request) {
        String databaseType = request.getDatabaseType().toLowerCase(Locale.ROOT);
        String cacheType = StringUtils.isNotEmpty(request.getCacheType())
                ? request.getCacheType().toLowerCase(Locale.ROOT) : "local";
        List<String> lines = new ArrayList<>();
        lines.add("cat2bug.bootstrap.mode=false");
        lines.add("cat2bug.cache.type=" + cacheType);
        if (StringUtils.isNotEmpty(request.getProfile())) {
            lines.add("cat2bug.profile=" + request.getProfile());
        }
        if (StringUtils.isNotEmpty(request.getTemp())) {
            lines.add("cat2bug.temp=" + request.getTemp());
        }
        if ("mysql".equals(databaseType)) {
            appendRuntimeMysqlLines(lines,
                    SetupDatabaseTestService.resolveMysqlUrl(toDatabaseTestRequest(request)),
                    request.getMysqlUsername(),
                    nullToEmpty(request.getMysqlPassword()));
        } else {
            String h2Database = DatabaseExistenceProbe.resolveH2DatabaseName(request);
            appendRuntimeH2Lines(lines,
                    DatabaseExistenceProbe.resolveH2JdbcUrl(h2Database),
                    "root",
                    "cat2bug_password");
        }
        appendFlywayLines(lines);
        if ("redis".equals(cacheType)) {
            appendRedisSessionLines(lines, request);
        }
        appendAiLinesFromSubmit(lines, request);
        return lines;
    }

    private static void appendRedisSessionLines(List<String> lines, SetupSubmitRequest request) {
        lines.add("cat2bug.session.store=redis");
        int port = request.getRedisPort() != null ? request.getRedisPort() : 6379;
        String host = StringUtils.isNotEmpty(request.getRedisHost()) ? request.getRedisHost() : "127.0.0.1";
        lines.add("cat2bug.redis.hosts=redis://" + host + ":" + port);
    }

    private static void appendAiLinesFromSubmit(List<String> lines, SetupSubmitRequest request) {
        lines.add("cat2bug.ai.enabled=" + Boolean.TRUE.equals(request.getAiEnabled()));
        if (StringUtils.isNotEmpty(request.getAiHost())) {
            lines.add("cat2bug.ai.host=" + request.getAiHost().trim());
        }
        if (StringUtils.isNotEmpty(request.getAiDefaultModel())) {
            lines.add("cat2bug.ai.default-business-model=" + request.getAiDefaultModel().trim());
        }
    }

    private static void appendAiLines(List<String> lines, String configPath) {
        lines.add("cat2bug.ai.enabled=" + InstallRuntimeConfigLoader.readAiEnabled(configPath));
        String host = InstallRuntimeConfigLoader.readAiHost(configPath);
        if (StringUtils.isNotEmpty(host)) {
            lines.add("cat2bug.ai.host=" + host.trim());
        }
    }

    private static void appendDatabaseLines(List<String> lines, InstallRuntimeConfigLoader.DatabaseConnection connection) {
        String url = connection.jdbcUrl().toLowerCase(Locale.ROOT);
        if (url.contains(":mysql:")) {
            appendRuntimeMysqlLines(lines, connection.jdbcUrl(), connection.username(), nullToEmpty(connection.password()));
        } else {
            appendRuntimeH2Lines(lines, connection.jdbcUrl(), connection.username(), nullToEmpty(connection.password()));
        }
    }

    private static void appendRuntimeMysqlLines(List<String> lines, String jdbcUrl, String username, String password) {
        lines.add("quarkus.datasource.active=false");
        lines.add("quarkus.datasource.runtime-h2.active=false");
        lines.add("quarkus.datasource.runtime-mysql.active=true");
        // db-kind / database-id 为 build-time：MySQL 生产包须用 -Dquarkus.profile=mysql 打包
        lines.add("quarkus.datasource.runtime-mysql.db-kind=mysql");
        lines.add("quarkus.datasource.runtime-mysql.jdbc.driver=com.mysql.cj.jdbc.Driver");
        lines.add("quarkus.datasource.runtime-mysql.jdbc.url=" + jdbcUrl);
        lines.add("quarkus.datasource.runtime-mysql.username=" + username);
        lines.add("quarkus.datasource.runtime-mysql.password=" + password);
        lines.add(QuarkusMapperDataSources.databaseIdConfigKey(QuarkusMapperDataSources.RUNTIME_MYSQL) + "=mysql");
    }

    /** H2 文件库与 MySQL 共用 {@code runtime-mysql} 命名池（MyBatis build-time 固定该数据源）。 */
    private static void appendRuntimeH2Lines(List<String> lines, String jdbcUrl, String username, String password) {
        lines.add("quarkus.datasource.active=false");
        lines.add("quarkus.datasource.runtime-h2.active=false");
        lines.add("quarkus.datasource.runtime-mysql.active=true");
        // 默认 fast-jar 按 H2 build；显式 driver 防止残留 mysql 驱动
        lines.add("quarkus.datasource.runtime-mysql.db-kind=h2");
        lines.add("quarkus.datasource.runtime-mysql.jdbc.driver=org.h2.Driver");
        lines.add("quarkus.datasource.runtime-mysql.jdbc.url=" + absolutizeH2FileUrl(jdbcUrl));
        lines.add("quarkus.datasource.runtime-mysql.username=" + username);
        lines.add("quarkus.datasource.runtime-mysql.password=" + password);
        lines.add(QuarkusMapperDataSources.databaseIdConfigKey(QuarkusMapperDataSources.RUNTIME_MYSQL) + "=h2");
    }
    /**
     * 把 {@code jdbc:h2:file:./data/xxx} 解析为绝对路径写入运行期配置。
     * Native 下 H2 解析相对路径的基准可能不是进程工作目录（构建期固化），
     * 会导致重启后连到另一个空库；绝对路径在 JVM / Native 下行为一致。
     */
    public static String absolutizeH2FileUrl(String jdbcUrl) {
        final String prefix = "jdbc:h2:file:";
        if (jdbcUrl == null || !jdbcUrl.startsWith(prefix)) {
            return jdbcUrl;
        }
        String rest = jdbcUrl.substring(prefix.length());
        int semi = rest.indexOf(';');
        String filePart = semi >= 0 ? rest.substring(0, semi) : rest;
        String options = semi >= 0 ? rest.substring(semi) : "";
        if (filePart.isEmpty() || filePart.startsWith("~") || Path.of(filePart).isAbsolute()) {
            return jdbcUrl;
        }
        String absolute = Path.of(filePart).toAbsolutePath().normalize().toString().replace('\\', '/');
        return prefix + absolute + options;
    }

    private Path writeLines(List<String> lines) throws IOException {
        Path target = runtimePropertiesPath();
        Files.createDirectories(target.getParent());
        Files.writeString(target, String.join(System.lineSeparator(), lines) + System.lineSeparator(), StandardCharsets.UTF_8);
        Files.writeString(pendingRestartMarkerPath(), "pending" + System.lineSeparator(), StandardCharsets.UTF_8);
        return target;
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private static com.cat2bug.common.domain.setup.SetupDatabaseTestRequest toDatabaseTestRequest(SetupSubmitRequest request) {
        com.cat2bug.common.domain.setup.SetupDatabaseTestRequest test =
                new com.cat2bug.common.domain.setup.SetupDatabaseTestRequest();
        test.setDatabaseType("mysql");
        test.setHost(request.getMysqlHost());
        test.setPort(request.getMysqlPort());
        test.setDatabase(request.getMysqlDatabase());
        test.setUsername(request.getMysqlUsername());
        test.setPassword(request.getMysqlPassword());
        return test;
    }
}
