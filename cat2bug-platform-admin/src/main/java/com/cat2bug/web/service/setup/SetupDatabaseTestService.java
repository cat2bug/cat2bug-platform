package com.cat2bug.web.service.setup;

import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.web.domain.setup.SetupDatabaseTestRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 数据库 JDBC 连接测试
 */
@Service
public class SetupDatabaseTestService
{
    private static final int LOGIN_TIMEOUT_SECONDS = 10;

    @Autowired
    private DatabaseExistenceProbe databaseExistenceProbe;

    public Map<String, Object> test(SetupDatabaseTestRequest request)
    {
        Map<String, Object> result = new HashMap<>(3);
        if (request == null || StringUtils.isEmpty(request.getDatabaseType()))
        {
            result.put("success", false);
            result.put("message", SetupMessages.msg("setup.test.database.type.empty"));
            return result;
        }
        try
        {
            String databaseType = request.getDatabaseType().toLowerCase();
            String jdbcUrl;
            String driver;
            String username;
            String password;
            if ("h2".equals(databaseType))
            {
                String databaseName = DatabaseExistenceProbe.resolveH2DatabaseName(request);
                DatabaseExistenceProbe.assertValidDatabaseName(databaseName);
                driver = "org.h2.Driver";
                jdbcUrl = StringUtils.isNotEmpty(request.getJdbcUrl())
                        ? request.getJdbcUrl()
                        : DatabaseExistenceProbe.resolveH2JdbcUrl(databaseName);
                username = StringUtils.isNotEmpty(request.getUsername()) ? request.getUsername() : "root";
                password = request.getPassword() != null ? request.getPassword() : "cat2bug_password";
                Class.forName(driver);
                DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
                try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password))
                {
                    if (connection.isValid(LOGIN_TIMEOUT_SECONDS))
                    {
                        result.put("success", true);
                        result.put("message", SetupMessages.msg("setup.test.connection.success"));
                        result.put("databaseMode", databaseExistenceProbe.probeH2Mode(connection));
                    }
                    else
                    {
                        result.put("success", false);
                        result.put("message", SetupMessages.msg("setup.test.connection.invalid"));
                    }
                }
            }
            else if ("mysql".equals(databaseType))
            {
                return testMysql(request);
            }
            else
            {
                result.put("success", false);
                result.put("message", SetupMessages.msg("setup.test.database.type.unsupported", request.getDatabaseType()));
            }
        }
        catch (Exception e)
        {
            result.put("success", false);
            result.put("message", formatConnectionError(e));
        }
        return result;
    }

    private Map<String, Object> testMysql(SetupDatabaseTestRequest request)
    {
        Map<String, Object> result = new HashMap<>(3);
        if (StringUtils.isEmpty(request.getDatabase()))
        {
            result.put("success", false);
            result.put("message", SetupMessages.msg("setup.test.database.name.empty"));
            return result;
        }
        String database = request.getDatabase().trim();
        DatabaseExistenceProbe.assertValidDatabaseName(database);
        String driver = "com.mysql.cj.jdbc.Driver";
        String username = request.getUsername();
        String password = request.getPassword();
        String jdbcUrl = resolveMysqlServerUrl(request);
        try
        {
            Class.forName(driver);
            DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password))
            {
                if (!connection.isValid(LOGIN_TIMEOUT_SECONDS))
                {
                    result.put("success", false);
                    result.put("message", SetupMessages.msg("setup.test.connection.invalid"));
                    return result;
                }
                boolean exists = mysqlDatabaseExists(connection, database);
                result.put("success", true);
                result.put("databaseMode", exists ? DatabaseExistenceProbe.MODE_EXISTING : DatabaseExistenceProbe.MODE_NEW);
                result.put("message", exists
                        ? SetupMessages.msg("setup.test.connection.success")
                        : SetupMessages.msg("setup.test.database.mysql.server.ready"));
            }
        }
        catch (Exception e)
        {
            result.put("success", false);
            result.put("message", formatConnectionError(e));
        }
        return result;
    }

    static boolean mysqlDatabaseExists(Connection serverConnection, String database) throws Exception
    {
        String sql = "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?";
        try (PreparedStatement ps = serverConnection.prepareStatement(sql))
        {
            ps.setString(1, database);
            try (ResultSet rs = ps.executeQuery())
            {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static String formatConnectionError(Exception e)
    {
        if (isH2FileFormatMismatchError(e))
        {
            return SetupMessages.msg("setup.test.database.h2.format.mismatch");
        }
        Throwable root = e;
        while (root.getCause() != null)
        {
            root = root.getCause();
        }
        String detail = root.getMessage() != null ? root.getMessage() : e.getMessage();
        String lower = detail != null ? detail.toLowerCase(Locale.ROOT) : "";
        if (lower.contains("connection refused") || lower.contains("communications link failure"))
        {
            return SetupMessages.msg("setup.test.database.refused");
        }
        if (lower.contains("access denied"))
        {
            return SetupMessages.msg("setup.test.database.access.denied");
        }
        if (lower.contains("unknown database"))
        {
            return SetupMessages.msg("setup.test.database.unknown.database");
        }
        if (lower.contains("could not create connection"))
        {
            return SetupMessages.msg("setup.test.database.connect.failed");
        }
        return SetupMessages.msg("setup.test.connection.failed", detail != null ? detail : e.getClass().getSimpleName());
    }

    static String resolveMysqlUrl(SetupDatabaseTestRequest request)
    {
        if (StringUtils.isNotEmpty(request.getJdbcUrl()))
        {
            return request.getJdbcUrl();
        }
        String host = StringUtils.isNotEmpty(request.getHost()) ? request.getHost() : "127.0.0.1";
        int port = request.getPort() != null ? request.getPort() : 3306;
        String database = StringUtils.isNotEmpty(request.getDatabase()) ? request.getDatabase() : "cat2bug_platform";
        return buildMysqlJdbcUrl(host, port, database);
    }

    static String resolveMysqlServerUrl(SetupDatabaseTestRequest request)
    {
        String host = StringUtils.isNotEmpty(request.getHost()) ? request.getHost() : "127.0.0.1";
        int port = request.getPort() != null ? request.getPort() : 3306;
        return buildMysqlJdbcUrl(host, port, null);
    }

    private static String buildMysqlJdbcUrl(String host, int port, String database)
    {
        String path = StringUtils.isNotEmpty(database) ? "/" + database : "/";
        return "jdbc:mysql://" + host + ":" + port + path
                + "?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2B8";
    }

    static boolean isUnknownDatabaseError(Throwable error)
    {
        Throwable current = error;
        while (current != null)
        {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("unknown database"))
            {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * H2 1.x 文件格式（write format 2）无法用 H2 2.x 直接打开写入。
     */
    static boolean isH2FileFormatMismatchError(Throwable error)
    {
        Throwable current = error;
        while (current != null)
        {
            String message = current.getMessage();
            if (message != null)
            {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("write format") && lower.contains("supported format"))
                {
                    return true;
                }
                if (lower.contains("file version") && lower.contains("is not supported"))
                {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
