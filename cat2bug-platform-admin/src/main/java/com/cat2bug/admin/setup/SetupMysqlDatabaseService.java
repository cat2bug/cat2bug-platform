package com.cat2bug.admin.setup;

import com.cat2bug.common.domain.setup.SetupDatabaseTestRequest;
import com.cat2bug.common.domain.setup.SetupSubmitRequest;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.setup.SetupMessages;
import com.cat2bug.common.util.FlywaySchemaSupport;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@ApplicationScoped
public class SetupMysqlDatabaseService {

    private static final Logger log = Logger.getLogger(SetupMysqlDatabaseService.class);
    private static final int LOGIN_TIMEOUT_SECONDS = 30;

    public MysqlDatabasePrepareResult prepare(SetupSubmitRequest request) {
        DatabaseExistenceProbe.assertValidDatabaseName(request.getMysqlDatabase());
        String database = request.getMysqlDatabase().trim();
        boolean existed = databaseExists(request, database);
        if (existed) {
            if (isSchemaPresent(request, database)) {
                return MysqlDatabasePrepareResult.existed(SetupMessages.msg("setup.mysql.schema.existing.hint"));
            }
            log.infof("MySQL 数据库 %s 已存在且无 Flyway 迁移记录，完成安装时将执行 Flyway 迁移", database);
            return MysqlDatabasePrepareResult.existed(null);
        }
        createDatabase(request, database);
        log.infof("MySQL 数据库 %s 已创建，完成安装时将执行 Flyway 迁移", database);
        return MysqlDatabasePrepareResult.created();
    }

    public boolean databaseExists(SetupSubmitRequest request, String database) {
        String sql = "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?";
        try (Connection connection = openServerConnection(request);
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, database);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            throw new ApiException(SetupMessages.msg("setup.mysql.detect.failed", rootMessage(e)));
        }
    }

    public void createDatabase(SetupSubmitRequest request, String database) {
        String ddl = "CREATE DATABASE `" + database + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci";
        try (Connection connection = openServerConnection(request);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(ddl);
        } catch (Exception e) {
            throw new ApiException(SetupMessages.msg("setup.mysql.create.failed", rootMessage(e)));
        }
    }

    public boolean isSchemaPresent(SetupSubmitRequest request, String database) {
        try (Connection connection = openDatabaseConnection(request, database)) {
            return FlywaySchemaSupport.hasSuccessfulMigration(connection);
        } catch (Exception e) {
            return false;
        }
    }

    private Connection openServerConnection(SetupSubmitRequest request) throws Exception {
        return openConnection(SetupDatabaseTestService.resolveMysqlServerUrl(toDatabaseTestRequest(request)),
                request.getMysqlUsername(), request.getMysqlPassword());
    }

    private Connection openDatabaseConnection(SetupSubmitRequest request, String database) throws Exception {
        SetupDatabaseTestRequest test = toDatabaseTestRequest(request);
        test.setDatabase(database);
        return openConnection(SetupDatabaseTestService.resolveMysqlUrl(test),
                request.getMysqlUsername(), request.getMysqlPassword());
    }

    private static Connection openConnection(String jdbcUrl, String username, String password) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
        Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
        if (!connection.isValid(LOGIN_TIMEOUT_SECONDS)) {
            connection.close();
            throw new ApiException(SetupMessages.msg("setup.mysql.connection.invalid"));
        }
        return connection;
    }

    private static SetupDatabaseTestRequest toDatabaseTestRequest(SetupSubmitRequest request) {
        SetupDatabaseTestRequest test = new SetupDatabaseTestRequest();
        test.setDatabaseType("mysql");
        test.setHost(request.getMysqlHost());
        test.setPort(request.getMysqlPort());
        test.setDatabase(request.getMysqlDatabase());
        test.setUsername(request.getMysqlUsername());
        test.setPassword(request.getMysqlPassword());
        return test;
    }

    private static String rootMessage(Exception e) {
        Throwable root = e;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        return message != null ? message : e.getClass().getSimpleName();
    }

    public record MysqlDatabasePrepareResult(boolean databaseExisted, String warningMessage) {
        static MysqlDatabasePrepareResult existed(String warning) {
            return new MysqlDatabasePrepareResult(true, warning);
        }

        static MysqlDatabasePrepareResult created() {
            return new MysqlDatabasePrepareResult(false, null);
        }
    }
}
