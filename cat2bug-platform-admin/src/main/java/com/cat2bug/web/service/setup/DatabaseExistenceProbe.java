package com.cat2bug.web.service.setup;

import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.FlywaySchemaSupport;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.web.domain.setup.SetupDatabaseTestRequest;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * 安装向导：按库/文件是否存在判定 new 或 existing 模式。
 */
@Component
public class DatabaseExistenceProbe
{
    public static final String MODE_NEW = "new";

    public static final String MODE_EXISTING = "existing";

    public static final String DEFAULT_DATABASE_NAME = "cat2bug_platform";

    @Autowired
    private SetupMysqlDatabaseService setupMysqlDatabaseService;

    public static void assertValidDatabaseName(String database)
    {
        if (StringUtils.isEmpty(database) || !database.matches("^[A-Za-z0-9_]+$"))
        {
            throw new ServiceException(SetupMessages.msg("setup.test.database.name.invalid"));
        }
    }

    public static String resolveH2DatabaseName(SetupSubmitRequest request)
    {
        if (request != null && StringUtils.isNotEmpty(request.getH2Database()))
        {
            return request.getH2Database().trim();
        }
        return DEFAULT_DATABASE_NAME;
    }

    public static String resolveH2DatabaseName(SetupDatabaseTestRequest request)
    {
        if (request != null && StringUtils.isNotEmpty(request.getDatabase()))
        {
            return request.getDatabase().trim();
        }
        return DEFAULT_DATABASE_NAME;
    }

    public static String resolveH2JdbcUrl(String databaseName)
    {
        assertValidDatabaseName(databaseName);
        return "jdbc:h2:file:./data/" + databaseName + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;";
    }

    /** 与 {@link #h2DataFile(String)} 指向同一库文件，供探测连接使用。 */
    public String resolveH2JdbcUrlForDatabase(String databaseName)
    {
        assertValidDatabaseName(databaseName);
        Path dataFile = h2DataFile(databaseName);
        String baseName = dataFile.getFileName().toString();
        if (baseName.endsWith(".mv.db"))
        {
            baseName = baseName.substring(0, baseName.length() - ".mv.db".length());
        }
        Path jdbcBase = dataFile.getParent().resolve(baseName);
        return "jdbc:h2:file:" + jdbcBase.toAbsolutePath() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;";
    }

    public Path h2DataFile(String databaseName)
    {
        assertValidDatabaseName(databaseName);
        return Paths.get("data", databaseName + ".mv.db");
    }

    public boolean h2Exists(String databaseName)
    {
        return Files.exists(h2DataFile(databaseName));
    }

    public String probeH2Mode(String databaseName)
    {
        if (!h2Exists(databaseName))
        {
            return MODE_NEW;
        }
        try
        {
            Class.forName("org.h2.Driver");
            try (Connection connection = DriverManager.getConnection(
                    resolveH2JdbcUrlForDatabase(databaseName), defaultH2Username(), defaultH2Password()))
            {
                return probeH2Mode(connection);
            }
        }
        catch (Exception e)
        {
            return MODE_NEW;
        }
    }

    /**
     * 已有库：H2 文件存在且业务表已初始化；仅 Flyway baseline 或空库仍视为 new。
     */
    public static String probeH2Mode(Connection connection)
    {
        return FlywaySchemaSupport.hasCoreSchema(connection) ? MODE_EXISTING : MODE_NEW;
    }

    static String defaultH2Username()
    {
        return "root";
    }

    static String defaultH2Password()
    {
        return "cat2bug_password";
    }

    public boolean mysqlSchemaExists(Connection serverConnection, String database) throws Exception
    {
        return SetupDatabaseTestService.mysqlDatabaseExists(serverConnection, database);
    }

    public String probeMysqlMode(SetupSubmitRequest request)
    {
        String database = request.getMysqlDatabase().trim();
        assertValidDatabaseName(database);
        return setupMysqlDatabaseService.databaseExists(request, database) ? MODE_EXISTING : MODE_NEW;
    }

    public String probeMode(SetupSubmitRequest request)
    {
        String dbType = request.getDatabaseType().toLowerCase();
        if ("mysql".equals(dbType))
        {
            return probeMysqlMode(request);
        }
        if ("h2".equals(dbType))
        {
            return probeH2Mode(resolveH2DatabaseName(request));
        }
        throw new ServiceException(SetupMessages.msg("setup.install.db.type.unsupported"));
    }

    public static void assertValidDatabaseMode(String databaseMode)
    {
        if (!MODE_NEW.equals(databaseMode) && !MODE_EXISTING.equals(databaseMode))
        {
            throw new ServiceException(SetupMessages.msg("setup.install.database.mode.invalid"));
        }
    }
}
