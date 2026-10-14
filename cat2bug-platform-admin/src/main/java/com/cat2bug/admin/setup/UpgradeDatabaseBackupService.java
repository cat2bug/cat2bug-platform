package com.cat2bug.admin.setup;

import com.cat2bug.common.domain.setup.SetupSubmitRequest;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.setup.SetupMessages;
import com.cat2bug.common.util.StringUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 升级向导：升级前数据库 SQL 备份（纯 JDBC）。
 */
@ApplicationScoped
public class UpgradeDatabaseBackupService {

    private static final Logger log = Logger.getLogger(UpgradeDatabaseBackupService.class);
    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final Pattern SAFE_FILE_NAME = Pattern.compile("^[A-Za-z0-9_\\-]+\\.sql$");

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

    @ConfigProperty(name = "quarkus.datasource.runtime-mysql.active", defaultValue = "false")
    boolean runtimeMysqlActive;

    @ConfigProperty(name = "quarkus.datasource.runtime-h2.active", defaultValue = "false")
    boolean runtimeH2Active;

    @Inject
    SetupSubmitDataSourceFactory setupSubmitDataSourceFactory;

    public Path resolveBackupDirectory() {
        Path dir = Paths.get("./backup/upgrade").toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new ApiException(SetupMessages.msg("upgrade.backup.dir.failed", e.getMessage()));
        }
        return dir;
    }

    public String resolveDatabaseName(SetupSubmitRequest request) {
        if (SetupSubmitDataSourceFactory.isMysql(request)) {
            if (StringUtils.isNotEmpty(request.getMysqlDatabase())) {
                return request.getMysqlDatabase().trim();
            }
            return DatabaseExistenceProbe.DEFAULT_DATABASE_NAME;
        }
        return DatabaseExistenceProbe.resolveH2DatabaseName(request);
    }

    public String defaultBackupFileName(SetupSubmitRequest request) {
        String databaseName = sanitizeDatabaseToken(resolveDatabaseName(request));
        return databaseName + "_" + LocalDateTime.now().format(BACKUP_TIME) + ".sql";
    }

    public Path resolveBackupFile(SetupSubmitRequest request, String fileName) {
        String normalized = normalizeFileName(fileName, resolveDatabaseName(request));
        Path dir = resolveBackupDirectory();
        Path target = dir.resolve(normalized).normalize();
        if (!target.startsWith(dir)) {
            throw new ApiException(SetupMessages.msg("upgrade.backup.file.invalid"));
        }
        return target;
    }

    public Path backup(SetupSubmitRequest request, String fileName) {
        Path target = resolveBackupFile(request, fileName);
        if (SetupSubmitDataSourceFactory.isMysql(request)) {
            backupMysql(request, target);
        } else {
            backupH2(request, target);
        }
        log.infof("升级前数据库备份完成: %s", target);
        return target;
    }

    public void restore(SetupSubmitRequest request, Path backupFile) {
        if (backupFile == null || !Files.isRegularFile(backupFile)) {
            throw new ApiException(SetupMessages.msg("upgrade.backup.file.missing"));
        }
        if (SetupSubmitDataSourceFactory.isMysql(request)) {
            restoreMysql(request, backupFile);
        } else {
            restoreH2(request, backupFile);
        }
        log.infof("数据库已从备份回滚: %s", backupFile);
    }

    static String normalizeFileName(String fileName, String databaseName) {
        String candidate = StringUtils.isNotEmpty(fileName) ? fileName.trim() : null;
        if (candidate == null) {
            candidate = sanitizeDatabaseToken(databaseName) + "_"
                    + LocalDateTime.now().format(BACKUP_TIME) + ".sql";
        }
        if (!candidate.toLowerCase(Locale.ROOT).endsWith(".sql")) {
            candidate = candidate + ".sql";
        }
        if (!SAFE_FILE_NAME.matcher(candidate).matches()) {
            throw new ApiException(SetupMessages.msg("upgrade.backup.file.invalid"));
        }
        return candidate;
    }

    private static String sanitizeDatabaseToken(String databaseName) {
        String token = databaseName == null ? "database" : databaseName.trim();
        token = token.replaceAll("[^A-Za-z0-9_\\-]", "_");
        return token.isEmpty() ? "database" : token;
    }

    private void backupMysql(SetupSubmitRequest request, Path target) {
        assertMysqlConnectionInfo(request);
        var ds = setupSubmitDataSourceFactory.createMysqlDataSource(request);
        try (ds; Connection connection = ds.getConnection()) {
            JdbcDatabaseSqlBackupSupport.exportDatabase(
                    connection,
                    request.getMysqlDatabase().trim(),
                    target,
                    JdbcDatabaseSqlBackupSupport.Dialect.MYSQL);
        } catch (SQLException | IOException e) {
            throw new ApiException(SetupMessages.msg("upgrade.backup.mysql.failed") + ": " + rootMessage(e));
        }
    }

    private void restoreMysql(SetupSubmitRequest request, Path backupFile) {
        assertMysqlConnectionInfo(request);
        var ds = setupSubmitDataSourceFactory.createMysqlDataSource(request);
        try (ds; Connection connection = ds.getConnection()) {
            JdbcDatabaseSqlBackupSupport.restoreDatabase(
                    connection,
                    backupFile,
                    JdbcDatabaseSqlBackupSupport.Dialect.MYSQL);
        } catch (SQLException | IOException e) {
            throw new ApiException(SetupMessages.msg("upgrade.rollback.mysql.failed") + ": " + rootMessage(e));
        }
    }

    private void restoreH2(SetupSubmitRequest request, Path backupFile) {
        try (Connection connection = openH2Connection(request)) {
            JdbcDatabaseSqlBackupSupport.restoreDatabase(
                    connection,
                    backupFile,
                    JdbcDatabaseSqlBackupSupport.Dialect.H2);
        } catch (SQLException | IOException e) {
            throw new ApiException(SetupMessages.msg("upgrade.rollback.h2.failed", rootMessage(e)));
        }
    }

    private void backupH2(SetupSubmitRequest request, Path target) {
        try (Connection connection = openH2Connection(request)) {
            String schema = JdbcDatabaseSqlBackupSupport.resolveH2Schema(connection);
            JdbcDatabaseSqlBackupSupport.exportDatabase(
                    connection,
                    schema,
                    target,
                    JdbcDatabaseSqlBackupSupport.Dialect.H2);
        } catch (SQLException | IOException e) {
            throw new ApiException(SetupMessages.msg("upgrade.backup.h2.failed", rootMessage(e)));
        }
    }

    private Connection openH2Connection(SetupSubmitRequest request) throws SQLException {
        String url = resolveH2JdbcUrl(request);
        return DriverManager.getConnection(url, resolveH2Username(), resolveH2Password().orElse(""));
    }

    private String resolveH2JdbcUrl(SetupSubmitRequest request) {
        if (runtimeH2Active) {
            return runtimeH2JdbcUrl.orElseGet(() -> DatabaseExistenceProbe.resolveH2JdbcUrl(
                    DatabaseExistenceProbe.resolveH2DatabaseName(request)));
        }
        String fromConfig = defaultJdbcUrl.orElse(null);
        if (StringUtils.isNotEmpty(fromConfig) && fromConfig.toLowerCase(Locale.ROOT).contains("jdbc:h2:")) {
            return fromConfig;
        }
        return DatabaseExistenceProbe.resolveH2JdbcUrl(DatabaseExistenceProbe.resolveH2DatabaseName(request));
    }

    private String resolveH2Username() {
        return runtimeH2Active ? runtimeH2Username : defaultUsername;
    }

    private Optional<String> resolveH2Password() {
        return runtimeH2Active ? runtimeH2Password : defaultPassword;
    }

    private static void assertMysqlConnectionInfo(SetupSubmitRequest request) {
        if (StringUtils.isEmpty(request.getMysqlHost())
                || request.getMysqlPort() == null
                || StringUtils.isEmpty(request.getMysqlDatabase())
                || StringUtils.isEmpty(request.getMysqlUsername())) {
            throw new ApiException(SetupMessages.msg("setup.install.mysql.incomplete"));
        }
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : error.toString();
    }
}
