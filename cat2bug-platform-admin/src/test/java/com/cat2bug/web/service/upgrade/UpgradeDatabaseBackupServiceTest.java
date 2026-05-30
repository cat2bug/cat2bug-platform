package com.cat2bug.web.service.upgrade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpgradeDatabaseBackupServiceTest
{
    @Test
    void normalizeFileName_usesDefaultWhenBlank()
    {
        String name = UpgradeDatabaseBackupService.normalizeFileName(null, "cat2bug_platform");
        assertTrue(name.startsWith("cat2bug_platform_"));
        assertTrue(name.endsWith(".sql"));
    }

    @Test
    void normalizeFileName_appendsSqlExtension()
    {
        String name = UpgradeDatabaseBackupService.normalizeFileName("before_upgrade", "cat2bug_platform");
        assertEquals("before_upgrade.sql", name);
    }

    @Test
    void isSafeBackupFileName_rejectsInvalidCharacters()
    {
        assertFalse(UpgradeDatabaseBackupService.isSafeBackupFileName("bad name.sql"));
        assertFalse(UpgradeDatabaseBackupService.isSafeBackupFileName("../evil.sql"));
        assertTrue(UpgradeDatabaseBackupService.isSafeBackupFileName("cat2bug_platform_20260101.sql"));
    }

    @Test
    void sanitizeDatabaseToken_replacesUnsafeCharacters()
    {
        assertEquals("cat2bug_platform", UpgradeDatabaseBackupService.sanitizeDatabaseToken("cat2bug_platform"));
        assertEquals("db_name_", UpgradeDatabaseBackupService.sanitizeDatabaseToken("db@name!"));
    }
}

class JdbcDatabaseSqlBackupSupportTest
{
    @TempDir
    Path tempDir;

    @Test
    void splitSqlStatements_respectsQuotedSemicolons() throws Exception
    {
        String sql = "INSERT INTO t (c) VALUES ('a;b');\nSELECT 1;";
        List<String> statements = JdbcDatabaseSqlBackupSupport.splitSqlStatements(sql);
        assertEquals(2, statements.size());
        assertTrue(statements.get(0).contains("'a;b'"));
        assertEquals("SELECT 1", statements.get(1).trim());
    }

    @Test
    void exportAndRestore_roundTripWithH2(@TempDir Path workDir) throws Exception
    {
        Path dbPath = workDir.resolve("backup_test");
        String url = "jdbc:h2:file:" + dbPath.toAbsolutePath()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;";
        try (Connection connection = DriverManager.getConnection(url, "root", "cat2bug_password");
                Statement statement = connection.createStatement())
        {
            statement.execute("CREATE TABLE demo_backup (id INT PRIMARY KEY, name VARCHAR(32))");
            statement.execute("INSERT INTO demo_backup VALUES (1, 'alpha')");
            statement.execute("INSERT INTO demo_backup VALUES (2, 'beta;gamma')");
        }

        Path backupFile = workDir.resolve("demo.sql");
        try (Connection connection = DriverManager.getConnection(url, "root", "cat2bug_password"))
        {
            String schema = JdbcDatabaseSqlBackupSupport.resolveH2Schema(connection);
            JdbcDatabaseSqlBackupSupport.exportDatabase(
                    connection,
                    schema,
                    backupFile,
                    JdbcDatabaseSqlBackupSupport.Dialect.H2);
        }
        assertTrue(Files.size(backupFile) > 0);

        try (Connection connection = DriverManager.getConnection(url, "root", "cat2bug_password");
                Statement statement = connection.createStatement())
        {
            statement.execute("DROP TABLE demo_backup");
        }

        try (Connection connection = DriverManager.getConnection(url, "root", "cat2bug_password"))
        {
            JdbcDatabaseSqlBackupSupport.restoreDatabase(
                    connection,
                    backupFile,
                    JdbcDatabaseSqlBackupSupport.Dialect.H2);
        }

        try (Connection connection = DriverManager.getConnection(url, "root", "cat2bug_password");
                var ps = connection.prepareStatement("SELECT COUNT(*) FROM demo_backup");
                var rs = ps.executeQuery())
        {
            rs.next();
            assertEquals(2, rs.getInt(1));
        }
    }
}
