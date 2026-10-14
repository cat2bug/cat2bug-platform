package com.cat2bug.quarkus.admin.setup;

import com.cat2bug.quarkus.common.domain.setup.SetupDatabaseTestRequest;
import com.cat2bug.quarkus.common.domain.setup.SetupSubmitRequest;
import com.cat2bug.quarkus.common.exception.ApiException;
import com.cat2bug.quarkus.common.setup.SetupMessages;
import com.cat2bug.quarkus.common.util.FlywaySchemaSupport;
import com.cat2bug.quarkus.common.util.StringUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;

@ApplicationScoped
public class DatabaseExistenceProbe {

    public static final String MODE_NEW = "new";
    public static final String MODE_EXISTING = "existing";
    public static final String DEFAULT_DATABASE_NAME = "cat2bug_platform";

    @Inject
    SetupMysqlDatabaseService setupMysqlDatabaseService;

    public static void assertValidDatabaseName(String database) {
        if (StringUtils.isEmpty(database) || !database.matches("^[A-Za-z0-9_]+$")) {
            throw new ApiException(SetupMessages.msg("setup.test.database.name.invalid"));
        }
    }

    public static String resolveH2DatabaseName(SetupSubmitRequest request) {
        if (request != null && StringUtils.isNotEmpty(request.getH2Database())) {
            return request.getH2Database().trim();
        }
        return DEFAULT_DATABASE_NAME;
    }

    public static String resolveH2DatabaseName(SetupDatabaseTestRequest request) {
        if (request != null && StringUtils.isNotEmpty(request.getDatabase())) {
            return request.getDatabase().trim();
        }
        return DEFAULT_DATABASE_NAME;
    }

    public static String resolveH2JdbcUrl(String databaseName) {
        assertValidDatabaseName(databaseName);
        return "jdbc:h2:file:./data/" + databaseName + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;";
    }

    public String resolveH2JdbcUrlForDatabase(String databaseName) {
        assertValidDatabaseName(databaseName);
        Path dataFile = h2DataFile(databaseName);
        String baseName = dataFile.getFileName().toString();
        if (baseName.endsWith(".mv.db")) {
            baseName = baseName.substring(0, baseName.length() - ".mv.db".length());
        }
        Path jdbcBase = dataFile.getParent().resolve(baseName);
        return "jdbc:h2:file:" + jdbcBase.toAbsolutePath() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;";
    }

    public Path h2DataFile(String databaseName) {
        assertValidDatabaseName(databaseName);
        return Paths.get("data", databaseName + ".mv.db");
    }

    public boolean h2Exists(String databaseName) {
        return Files.exists(h2DataFile(databaseName));
    }

    public String probeH2Mode(String databaseName) {
        if (!h2Exists(databaseName)) {
            return MODE_NEW;
        }
        try {
            Class.forName("org.h2.Driver");
            try (Connection connection = DriverManager.getConnection(
                    resolveH2JdbcUrlForDatabase(databaseName), defaultH2Username(), defaultH2Password())) {
                return probeH2Mode(connection);
            }
        } catch (Exception e) {
            return MODE_NEW;
        }
    }

    public static String probeH2Mode(Connection connection) {
        return FlywaySchemaSupport.hasSuccessfulMigration(connection) ? MODE_EXISTING : MODE_NEW;
    }

    static String defaultH2Username() {
        return "root";
    }

    static String defaultH2Password() {
        return "cat2bug_password";
    }

    public String probeMysqlMode(SetupSubmitRequest request) {
        String database = request.getMysqlDatabase().trim();
        assertValidDatabaseName(database);
        return setupMysqlDatabaseService.databaseExists(request, database) ? MODE_EXISTING : MODE_NEW;
    }

    public String probeMode(SetupSubmitRequest request) {
        String dbType = request.getDatabaseType().toLowerCase();
        if ("mysql".equals(dbType)) {
            return probeMysqlMode(request);
        }
        if ("h2".equals(dbType)) {
            return probeH2Mode(resolveH2DatabaseName(request));
        }
        throw new ApiException(SetupMessages.msg("setup.install.db.type.unsupported"));
    }

    public static void assertValidDatabaseMode(String databaseMode) {
        if (!MODE_NEW.equals(databaseMode) && !MODE_EXISTING.equals(databaseMode)) {
            throw new ApiException(SetupMessages.msg("setup.install.database.mode.invalid"));
        }
    }
}
