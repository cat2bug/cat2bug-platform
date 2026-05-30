package com.cat2bug.web.service.upgrade;

import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import com.cat2bug.web.service.setup.DatabaseExistenceProbe;
import com.cat2bug.web.service.setup.SetupMessages;
import com.cat2bug.web.service.setup.SetupSubmitDataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

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
import java.util.regex.Pattern;

/**
 * 升级向导：升级前数据库 SQL 备份与失败回滚（纯 JDBC，不依赖外部命令）。
 */
@Service
public class UpgradeDatabaseBackupService
{
    private static final Logger log = LoggerFactory.getLogger(UpgradeDatabaseBackupService.class);

    private static final DateTimeFormatter BACKUP_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final Pattern SAFE_FILE_NAME = Pattern.compile("^[A-Za-z0-9_\\-]+\\.sql$");

    @Autowired
    private Environment environment;

    @Autowired
    private SetupSubmitDataSourceFactory setupSubmitDataSourceFactory;

    public Path resolveBackupDirectory()
    {
        Path dir = Paths.get("./backup/upgrade").toAbsolutePath().normalize();
        try
        {
            Files.createDirectories(dir);
        }
        catch (IOException e)
        {
            throw new ServiceException(SetupMessages.msg("upgrade.backup.dir.failed", e.getMessage()));
        }
        return dir;
    }

    public String resolveDatabaseName(SetupSubmitRequest request)
    {
        if (SetupSubmitDataSourceFactory.isMysql(request))
        {
            if (StringUtils.isNotEmpty(request.getMysqlDatabase()))
            {
                return request.getMysqlDatabase().trim();
            }
            return DatabaseExistenceProbe.DEFAULT_DATABASE_NAME;
        }
        return DatabaseExistenceProbe.resolveH2DatabaseName(request);
    }

    public String defaultBackupFileName(SetupSubmitRequest request)
    {
        String databaseName = sanitizeDatabaseToken(resolveDatabaseName(request));
        return databaseName + "_" + LocalDateTime.now().format(BACKUP_TIME) + ".sql";
    }

    public Path resolveBackupFile(SetupSubmitRequest request, String fileName)
    {
        String normalized = normalizeFileName(fileName, resolveDatabaseName(request));
        Path dir = resolveBackupDirectory();
        Path target = dir.resolve(normalized).normalize();
        if (!target.startsWith(dir))
        {
            throw new ServiceException(SetupMessages.msg("upgrade.backup.file.invalid"));
        }
        return target;
    }

    public Path backup(SetupSubmitRequest request, String fileName)
    {
        Path target = resolveBackupFile(request, fileName);
        if (SetupSubmitDataSourceFactory.isMysql(request))
        {
            backupMysql(request, target);
        }
        else
        {
            backupH2(request, target);
        }
        log.info("升级前数据库备份完成: {}", target);
        return target;
    }

    public void restore(SetupSubmitRequest request, Path backupFile)
    {
        if (backupFile == null || !Files.isRegularFile(backupFile))
        {
            throw new ServiceException(SetupMessages.msg("upgrade.backup.file.missing"));
        }
        if (SetupSubmitDataSourceFactory.isMysql(request))
        {
            restoreMysql(request, backupFile);
        }
        else
        {
            restoreH2(request, backupFile);
        }
        log.info("数据库已从备份回滚: {}", backupFile);
    }

    static boolean isSafeBackupFileName(String candidate)
    {
        return candidate != null && SAFE_FILE_NAME.matcher(candidate).matches();
    }

    static String normalizeFileName(String fileName, String databaseName)
    {
        String candidate = StringUtils.isNotEmpty(fileName) ? fileName.trim() : null;
        if (candidate == null)
        {
            candidate = sanitizeDatabaseToken(databaseName) + "_"
                    + LocalDateTime.now().format(BACKUP_TIME) + ".sql";
        }
        if (!candidate.toLowerCase(Locale.ROOT).endsWith(".sql"))
        {
            candidate = candidate + ".sql";
        }
        if (!SAFE_FILE_NAME.matcher(candidate).matches())
        {
            throw new ServiceException(SetupMessages.msg("upgrade.backup.file.invalid"));
        }
        return candidate;
    }

    static String sanitizeDatabaseToken(String databaseName)
    {
        String token = databaseName == null ? "database" : databaseName.trim();
        token = token.replaceAll("[^A-Za-z0-9_\\-]", "_");
        return token.isEmpty() ? "database" : token;
    }

    private void backupMysql(SetupSubmitRequest request, Path target)
    {
        assertMysqlConnectionInfo(request);
        var dataSource = setupSubmitDataSourceFactory.createMysqlDataSource(request);
        try (dataSource; Connection connection = dataSource.getConnection())
        {
            JdbcDatabaseSqlBackupSupport.exportDatabase(
                    connection,
                    request.getMysqlDatabase().trim(),
                    target,
                    JdbcDatabaseSqlBackupSupport.Dialect.MYSQL);
        }
        catch (SQLException | IOException e)
        {
            throw new ServiceException(SetupMessages.msg("upgrade.backup.mysql.failed") + ": " + rootMessage(e));
        }
    }

    private void restoreMysql(SetupSubmitRequest request, Path backupFile)
    {
        assertMysqlConnectionInfo(request);
        var dataSource = setupSubmitDataSourceFactory.createMysqlDataSource(request);
        try (dataSource; Connection connection = dataSource.getConnection())
        {
            JdbcDatabaseSqlBackupSupport.restoreDatabase(
                    connection,
                    backupFile,
                    JdbcDatabaseSqlBackupSupport.Dialect.MYSQL);
        }
        catch (SQLException | IOException e)
        {
            throw new ServiceException(SetupMessages.msg("upgrade.rollback.mysql.failed") + ": " + rootMessage(e));
        }
    }

    private void backupH2(SetupSubmitRequest request, Path target)
    {
        try (Connection connection = openH2Connection(request))
        {
            String schema = JdbcDatabaseSqlBackupSupport.resolveH2Schema(connection);
            JdbcDatabaseSqlBackupSupport.exportDatabase(
                    connection,
                    schema,
                    target,
                    JdbcDatabaseSqlBackupSupport.Dialect.H2);
        }
        catch (SQLException | IOException e)
        {
            throw new ServiceException(SetupMessages.msg("upgrade.backup.h2.failed", rootMessage(e)));
        }
    }

    private void restoreH2(SetupSubmitRequest request, Path backupFile)
    {
        try (Connection connection = openH2Connection(request))
        {
            JdbcDatabaseSqlBackupSupport.restoreDatabase(
                    connection,
                    backupFile,
                    JdbcDatabaseSqlBackupSupport.Dialect.H2);
        }
        catch (SQLException | IOException e)
        {
            throw new ServiceException(SetupMessages.msg("upgrade.rollback.h2.failed", rootMessage(e)));
        }
    }

    private Connection openH2Connection(SetupSubmitRequest request) throws SQLException
    {
        String url = resolveH2JdbcUrl(request);
        String username = environment != null
                ? environment.getProperty("spring.datasource.druid.master.username", "root") : "root";
        String password = environment != null
                ? environment.getProperty("spring.datasource.druid.master.password", "cat2bug_password")
                : "cat2bug_password";
        return DriverManager.getConnection(url, username, password);
    }

    private String resolveH2JdbcUrl(SetupSubmitRequest request)
    {
        String fromEnv = environment != null
                ? environment.getProperty("spring.datasource.druid.master.url") : null;
        if (StringUtils.isNotEmpty(fromEnv) && fromEnv.toLowerCase(Locale.ROOT).contains("jdbc:h2:"))
        {
            return fromEnv;
        }
        return DatabaseExistenceProbe.resolveH2JdbcUrl(DatabaseExistenceProbe.resolveH2DatabaseName(request));
    }

    private static void assertMysqlConnectionInfo(SetupSubmitRequest request)
    {
        if (StringUtils.isEmpty(request.getMysqlHost())
                || request.getMysqlPort() == null
                || StringUtils.isEmpty(request.getMysqlDatabase())
                || StringUtils.isEmpty(request.getMysqlUsername()))
        {
            throw new ServiceException(SetupMessages.msg("setup.install.mysql.incomplete"));
        }
    }

    private static String rootMessage(Throwable error)
    {
        Throwable current = error;
        while (current.getCause() != null)
        {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : error.toString();
    }
}
