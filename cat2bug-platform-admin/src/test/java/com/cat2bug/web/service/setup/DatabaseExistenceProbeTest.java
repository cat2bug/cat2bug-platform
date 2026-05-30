package com.cat2bug.web.service.setup;

import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.web.domain.setup.SetupDatabaseTestRequest;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseExistenceProbeTest
{
    @Mock
    private SetupMysqlDatabaseService setupMysqlDatabaseService;

    @TempDir
    Path workDir;

    private DatabaseExistenceProbe probe;

    @BeforeEach
    void setUp()
    {
        probe = SetupTestSupport.probeWithDataDir(workDir, setupMysqlDatabaseService);
    }

    @Test
    void assertValidDatabaseName_acceptsSimpleName()
    {
        DatabaseExistenceProbe.assertValidDatabaseName("cat2bug_platform");
    }

    @Test
    void assertValidDatabaseName_rejectsInvalidCharacters()
    {
        SetupTestSupport.runWithMessages(() ->
                assertThrows(ServiceException.class,
                        () -> DatabaseExistenceProbe.assertValidDatabaseName("cat2bug-platform")));
    }

    @Test
    void resolveH2DatabaseName_usesDefaultWhenMissing()
    {
        assertEquals(DatabaseExistenceProbe.DEFAULT_DATABASE_NAME,
                DatabaseExistenceProbe.resolveH2DatabaseName((SetupSubmitRequest) null));
        assertEquals(DatabaseExistenceProbe.DEFAULT_DATABASE_NAME,
                DatabaseExistenceProbe.resolveH2DatabaseName(new SetupSubmitRequest()));
    }

    @Test
    void resolveH2DatabaseName_trimsSubmitRequestValue()
    {
        SetupSubmitRequest request = new SetupSubmitRequest();
        request.setH2Database("  legacy_db  ");
        assertEquals("legacy_db", DatabaseExistenceProbe.resolveH2DatabaseName(request));
    }

    @Test
    void resolveH2DatabaseName_trimsTestRequestValue()
    {
        SetupDatabaseTestRequest request = new SetupDatabaseTestRequest();
        request.setDatabase("  attach_test  ");
        assertEquals("attach_test", DatabaseExistenceProbe.resolveH2DatabaseName(request));
    }

    @Test
    void resolveH2JdbcUrl_buildsFileUrl()
    {
        assertEquals(
                "jdbc:h2:file:./data/legacy_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;",
                DatabaseExistenceProbe.resolveH2JdbcUrl("legacy_db"));
    }

    @Test
    void h2Exists_returnsFalseWhenFileMissing()
    {
        assertFalse(probe.h2Exists("missing_db"));
    }

    @Test
    void h2Exists_returnsTrueWhenMvDbPresent() throws Exception
    {
        Path dataDir = workDir.resolve("data");
        Files.createDirectories(dataDir);
        Files.createFile(dataDir.resolve("legacy_db.mv.db"));

        assertTrue(probe.h2Exists("legacy_db"));
    }

    @Test
    void probeH2Mode_returnsNewWhenFileMissingOrEmpty() throws Exception
    {
        assertEquals(DatabaseExistenceProbe.MODE_NEW, probe.probeH2Mode("fresh_db"));

        Path dataDir = workDir.resolve("data");
        Files.createDirectories(dataDir);
        Files.createFile(dataDir.resolve("empty_db.mv.db"));

        assertEquals(DatabaseExistenceProbe.MODE_NEW, probe.probeH2Mode("empty_db"));
    }

    @Test
    void probeH2Mode_returnsExistingWhenSchemaPresent() throws Exception
    {
        String databaseName = "legacy_db";
        SetupSubmitRequest seedRequest = new SetupSubmitRequest();
        seedRequest.setDatabaseType("h2");
        seedRequest.setH2Database(databaseName);

        SetupSubmitDataSourceFactory dataSourceFactory = new SetupSubmitDataSourceFactory()
        {
            @Override
            public com.alibaba.druid.pool.DruidDataSource createH2DataSource(SetupSubmitRequest request)
            {
                com.alibaba.druid.pool.DruidDataSource dataSource = super.createH2DataSource(request);
                dataSource.setUrl(SetupTestSupport.h2JdbcUrl(workDir, DatabaseExistenceProbe.resolveH2DatabaseName(request)));
                return dataSource;
            }
        };
        SetupMigrationService migrationService = new SetupMigrationService();
        try (com.alibaba.druid.pool.DruidDataSource dataSource = dataSourceFactory.createH2DataSource(seedRequest))
        {
            migrationService.migrate("h2", dataSource);
        }

        assertEquals(DatabaseExistenceProbe.MODE_EXISTING, probe.probeH2Mode(databaseName));
    }

    @Test
    void probeMysqlMode_delegatesToMysqlService()
    {
        SetupSubmitRequest request = new SetupSubmitRequest();
        request.setMysqlHost("127.0.0.1");
        request.setMysqlPort(3306);
        request.setMysqlDatabase("cat2bug");
        request.setMysqlUsername("root");
        request.setMysqlPassword("secret");

        when(setupMysqlDatabaseService.databaseExists(request, "cat2bug")).thenReturn(true);
        assertEquals(DatabaseExistenceProbe.MODE_EXISTING, probe.probeMysqlMode(request));

        when(setupMysqlDatabaseService.databaseExists(request, "cat2bug")).thenReturn(false);
        assertEquals(DatabaseExistenceProbe.MODE_NEW, probe.probeMysqlMode(request));
    }

    @Test
    void probeMode_routesByDatabaseType()
    {
        SetupSubmitRequest h2Request = new SetupSubmitRequest();
        h2Request.setDatabaseType("h2");
        h2Request.setH2Database("fresh_db");
        assertEquals(DatabaseExistenceProbe.MODE_NEW, probe.probeMode(h2Request));

        SetupSubmitRequest mysqlRequest = new SetupSubmitRequest();
        mysqlRequest.setDatabaseType("mysql");
        mysqlRequest.setMysqlDatabase("cat2bug");
        when(setupMysqlDatabaseService.databaseExists(mysqlRequest, "cat2bug")).thenReturn(true);
        assertEquals(DatabaseExistenceProbe.MODE_EXISTING, probe.probeMode(mysqlRequest));
    }

    @Test
    void assertValidDatabaseMode_rejectsUnknownValue()
    {
        SetupTestSupport.runWithMessages(() ->
                assertThrows(ServiceException.class,
                        () -> DatabaseExistenceProbe.assertValidDatabaseMode("legacy")));
    }
}
