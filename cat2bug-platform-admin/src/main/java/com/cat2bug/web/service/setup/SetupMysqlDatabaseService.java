package com.cat2bug.web.service.setup;

import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.FlywaySchemaSupport;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.web.domain.setup.SetupDatabaseTestRequest;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * MySQL 安装阶段：检测/创建数据库（表结构由 Flyway 迁移初始化，与 H2 一致）。
 */
@Service
public class SetupMysqlDatabaseService
{
    private static final Logger log = LoggerFactory.getLogger(SetupMysqlDatabaseService.class);

    private static final int LOGIN_TIMEOUT_SECONDS = 30;

    public MysqlDatabasePrepareResult prepare(SetupSubmitRequest request)
    {
        assertValidDatabaseName(request.getMysqlDatabase());
        String database = request.getMysqlDatabase().trim();
        boolean existed = databaseExists(request, database);
        if (existed)
        {
            if (isSchemaPresent(request, database))
            {
                return MysqlDatabasePrepareResult.existed(SetupMessages.msg("setup.mysql.schema.existing.hint"));
            }
            log.info("MySQL 数据库 {} 已存在且无 Flyway 迁移记录，完成安装时将执行 Flyway 迁移", database);
            return MysqlDatabasePrepareResult.existed(null);
        }
        createDatabase(request, database);
        log.info("MySQL 数据库 {} 已创建，完成安装时将执行 Flyway 迁移", database);
        return MysqlDatabasePrepareResult.created();
    }

    public boolean databaseExists(SetupSubmitRequest request, String database)
    {
        String sql = "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?";
        try (Connection connection = openServerConnection(request);
                PreparedStatement ps = connection.prepareStatement(sql))
        {
            ps.setString(1, database);
            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
        catch (Exception e)
        {
            throw new ServiceException(SetupMessages.msg("setup.mysql.detect.failed", rootMessage(e)));
        }
    }

    public void createDatabase(SetupSubmitRequest request, String database)
    {
        String ddl = "CREATE DATABASE `" + database + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci";
        try (Connection connection = openServerConnection(request);
                Statement statement = connection.createStatement())
        {
            statement.executeUpdate(ddl);
        }
        catch (Exception e)
        {
            throw new ServiceException(SetupMessages.msg("setup.mysql.create.failed", rootMessage(e)));
        }
    }

    public boolean isSchemaPresent(SetupSubmitRequest request, String database)
    {
        try (Connection connection = openDatabaseConnection(request, database))
        {
            return FlywaySchemaSupport.hasSuccessfulMigration(connection);
        }
        catch (Exception e)
        {
            return false;
        }
    }

    static void assertValidDatabaseName(String database)
    {
        DatabaseExistenceProbe.assertValidDatabaseName(database);
    }

    private Connection openServerConnection(SetupSubmitRequest request) throws Exception
    {
        return openConnection(SetupDatabaseTestService.resolveMysqlServerUrl(toDatabaseTestRequest(request)),
                request.getMysqlUsername(), request.getMysqlPassword());
    }

    private Connection openDatabaseConnection(SetupSubmitRequest request, String database) throws Exception
    {
        SetupDatabaseTestRequest test = toDatabaseTestRequest(request);
        test.setDatabase(database);
        return openConnection(SetupDatabaseTestService.resolveMysqlUrl(test),
                request.getMysqlUsername(), request.getMysqlPassword());
    }

    private static Connection openConnection(String jdbcUrl, String username, String password) throws Exception
    {
        Class.forName("com.mysql.cj.jdbc.Driver");
        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
        Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
        if (!connection.isValid(LOGIN_TIMEOUT_SECONDS))
        {
            connection.close();
            throw new ServiceException(SetupMessages.msg("setup.mysql.connection.invalid"));
        }
        return connection;
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

    private static String rootMessage(Exception e)
    {
        Throwable root = e;
        while (root.getCause() != null)
        {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message != null ? message : e.getClass().getSimpleName();
    }

    public record MysqlDatabasePrepareResult(boolean databaseExisted, String warningMessage)
    {
        static MysqlDatabasePrepareResult existed(String warning)
        {
            return new MysqlDatabasePrepareResult(true, warning);
        }

        static MysqlDatabasePrepareResult created()
        {
            return new MysqlDatabasePrepareResult(false, null);
        }
    }
}
