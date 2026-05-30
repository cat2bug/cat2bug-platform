package com.cat2bug.web.service.setup;

import com.cat2bug.web.domain.setup.SetupDatabaseTestRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SetupDatabaseTestServiceTest
{
    @TempDir
    Path workDir;

    @Test
    void isUnknownDatabaseError_detectsMysqlMessage()
    {
        assertTrue(SetupDatabaseTestService.isUnknownDatabaseError(
                new RuntimeException("Unknown database 'missing'")));
    }

    @Test
    void isUnknownDatabaseError_returnsFalseForOtherErrors()
    {
        assertFalse(SetupDatabaseTestService.isUnknownDatabaseError(
                new RuntimeException("Access denied for user")));
    }

    @Test
    void mysqlDatabaseExists_returnsTrueWhenSchemaPresent() throws Exception
    {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1);

        assertTrue(SetupDatabaseTestService.mysqlDatabaseExists(connection, "cat2bug"));
    }

    @Test
    void mysqlDatabaseExists_returnsFalseWhenSchemaMissing() throws Exception
    {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(0);

        assertFalse(SetupDatabaseTestService.mysqlDatabaseExists(connection, "cat2bug"));
    }

    @Test
    void test_h2_returnsNewModeFromProbeAfterSuccessfulConnection()
    {
        SetupTestSupport.runWithMessages(() ->
        {
            DatabaseExistenceProbe probe = mock(DatabaseExistenceProbe.class);
            when(probe.probeH2Mode("fresh_attach")).thenReturn(DatabaseExistenceProbe.MODE_NEW);

            SetupDatabaseTestService service = new SetupDatabaseTestService();
            ReflectionTestUtils.setField(service, "databaseExistenceProbe", probe);

            SetupDatabaseTestRequest request = new SetupDatabaseTestRequest();
            request.setDatabaseType("h2");
            request.setDatabase("fresh_attach");
            request.setJdbcUrl(SetupTestSupport.h2JdbcUrl(workDir, "fresh_attach"));

            Map<String, Object> result = service.test(request);

            assertTrue(Boolean.TRUE.equals(result.get("success")));
            assertEquals(DatabaseExistenceProbe.MODE_NEW, result.get("databaseMode"));
        });
    }

    @Test
    void test_h2_returnsNewModeWhenMvDbPresentButEmpty() throws Exception
    {
        Path dataDir = workDir.resolve("data");
        Files.createDirectories(dataDir);
        Files.createFile(dataDir.resolve("legacy_attach.mv.db"));

        SetupTestSupport.runWithMessages(() ->
        {
            SetupDatabaseTestService service = new SetupDatabaseTestService();
            ReflectionTestUtils.setField(service, "databaseExistenceProbe",
                    SetupTestSupport.probeWithDataDir(workDir, null));

            SetupDatabaseTestRequest request = new SetupDatabaseTestRequest();
            request.setDatabaseType("h2");
            request.setDatabase("legacy_attach");
            request.setJdbcUrl(SetupTestSupport.h2JdbcUrl(workDir, "legacy_attach"));

            Map<String, Object> result = service.test(request);

            assertTrue(Boolean.TRUE.equals(result.get("success")));
            assertEquals(DatabaseExistenceProbe.MODE_NEW, result.get("databaseMode"));
        });
    }
}
