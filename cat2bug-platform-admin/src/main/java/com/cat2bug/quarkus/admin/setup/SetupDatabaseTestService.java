package com.cat2bug.quarkus.admin.setup;

import com.cat2bug.quarkus.common.domain.setup.SetupDatabaseTestRequest;
import com.cat2bug.quarkus.common.setup.SetupMessages;
import com.cat2bug.quarkus.common.util.StringUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class SetupDatabaseTestService {

    private static final int LOGIN_TIMEOUT_SECONDS = 10;

    @Inject
    DatabaseExistenceProbe databaseExistenceProbe;

    public Map<String, Object> test(SetupDatabaseTestRequest request) {
        Map<String, Object> result = new HashMap<>(3);
        if (request == null || StringUtils.isEmpty(request.getDatabaseType())) {
            result.put("success", false);
            result.put("message", SetupMessages.msg("setup.test.database.type.empty"));
            return result;
        }
        try {
            String databaseType = request.getDatabaseType().toLowerCase(Locale.ROOT);
            if ("h2".equals(databaseType)) {
                return testH2(request);
            }
            if ("mysql".equals(databaseType)) {
                return testMysql(request);
            }
            result.put("success", false);
            result.put("message", SetupMessages.msg("setup.test.database.type.unsupported", request.getDatabaseType()));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", formatConnectionError(e));
        }
        return result;
    }

    private Map<String, Object> testH2(SetupDatabaseTestRequest request) throws Exception {
        Map<String, Object> result = new HashMap<>(3);
        String databaseName = DatabaseExistenceProbe.resolveH2DatabaseName(request);
        DatabaseExistenceProbe.assertValidDatabaseName(databaseName);
        String driver = "org.h2.Driver";
        String jdbcUrl = StringUtils.isNotEmpty(request.getJdbcUrl())
                ? request.getJdbcUrl()
                : DatabaseExistenceProbe.resolveH2JdbcUrl(databaseName);
        String username = StringUtils.isNotEmpty(request.getUsername()) ? request.getUsername() : "root";
        String password = request.getPassword() != null ? request.getPassword() : "cat2bug_password";
        Class.forName(driver);
        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            if (connection.isValid(LOGIN_TIMEOUT_SECONDS)) {
                result.put("success", true);
                result.put("message", SetupMessages.msg("setup.test.connection.success"));
                result.put("databaseMode", databaseExistenceProbe.probeH2Mode(connection));
            } else {
                result.put("success", false);
                result.put("message", SetupMessages.msg("setup.test.connection.invalid"));
            }
        }
        return result;
    }

    private Map<String, Object> testMysql(SetupDatabaseTestRequest request) {
        Map<String, Object> result = new HashMap<>(3);
        if (StringUtils.isEmpty(request.getDatabase())) {
            result.put("success", false);
            result.put("message", SetupMessages.msg("setup.test.database.name.empty"));
            return result;
        }
        String database = request.getDatabase().trim();
        DatabaseExistenceProbe.assertValidDatabaseName(database);
        String jdbcUrl = resolveMysqlServerUrl(request);
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
            try (Connection connection = DriverManager.getConnection(jdbcUrl, request.getUsername(), request.getPassword())) {
                if (!connection.isValid(LOGIN_TIMEOUT_SECONDS)) {
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
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", formatConnectionError(e));
        }
        return result;
    }

    static boolean mysqlDatabaseExists(Connection serverConnection, String database) throws Exception {
        String sql = "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?";
        try (PreparedStatement ps = serverConnection.prepareStatement(sql)) {
            ps.setString(1, database);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static String formatConnectionError(Exception e) {
        if (isH2FileFormatMismatchError(e)) {
            return SetupMessages.msg("setup.test.database.h2.format.mismatch");
        }
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String detail = root.getMessage() != null ? root.getMessage() : e.getMessage();
        String lower = detail != null ? detail.toLowerCase(Locale.ROOT) : "";
        if (lower.contains("connection refused") || lower.contains("communications link failure")) {
            return SetupMessages.msg("setup.test.database.refused");
        }
        if (lower.contains("access denied")) {
            return SetupMessages.msg("setup.test.database.access.denied");
        }
        if (lower.contains("unknown database")) {
            return SetupMessages.msg("setup.test.database.unknown.database");
        }
        if (lower.contains("could not create connection")) {
            return SetupMessages.msg("setup.test.database.connect.failed");
        }
        return SetupMessages.msg("setup.test.connection.failed", detail != null ? detail : e.getClass().getSimpleName());
    }

    static String resolveMysqlUrl(SetupDatabaseTestRequest request) {
        if (StringUtils.isNotEmpty(request.getJdbcUrl())) {
            return request.getJdbcUrl();
        }
        String host = StringUtils.isNotEmpty(request.getHost()) ? request.getHost() : "127.0.0.1";
        int port = request.getPort() != null ? request.getPort() : 3306;
        String database = StringUtils.isNotEmpty(request.getDatabase()) ? request.getDatabase() : "cat2bug_platform";
        return buildMysqlJdbcUrl(host, port, database);
    }

    static String resolveMysqlServerUrl(SetupDatabaseTestRequest request) {
        String host = StringUtils.isNotEmpty(request.getHost()) ? request.getHost() : "127.0.0.1";
        int port = request.getPort() != null ? request.getPort() : 3306;
        return buildMysqlJdbcUrl(host, port, null);
    }

    private static String buildMysqlJdbcUrl(String host, int port, String database) {
        String path = StringUtils.isNotEmpty(database) ? "/" + database : "/";
        return "jdbc:mysql://" + host + ":" + port + path
                + "?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=GMT%2B8";
    }

    static boolean isH2FileFormatMismatchError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("write format") && lower.contains("supported format")) {
                    return true;
                }
                if (lower.contains("file version") && lower.contains("is not supported")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
