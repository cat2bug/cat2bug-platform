package com.cat2bug.web.service.setup;

import com.alibaba.druid.pool.DruidDataSource;
import com.cat2bug.common.utils.SecurityUtils;
import com.cat2bug.framework.service.InstallService;
import com.cat2bug.system.service.ISysConfigService;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * existing H2 附着：setup 阶段跳过 Flyway，并按向导提交更新 admin 凭证。
 */
@ExtendWith(MockitoExtension.class)
class SetupInstallServiceExistingH2Test
{
    private static final String DB_NAME = "attach_existing";

    @TempDir
    Path workDir;

    @Mock
    private InstallService installService;

    @Mock
    private SetupConfigWriter setupConfigWriter;

    @Spy
    private SetupMigrationService setupMigrationService;

    @Mock
    private SetupMysqlDatabaseService setupMysqlDatabaseService;

    @Mock
    private ISysConfigService configService;

    @Mock
    private InstallApplicationRestarter installApplicationRestarter;

    @Mock
    private InstallRuntimeActivator installRuntimeActivator;

    @InjectMocks
    private SetupInstallService setupInstallService;

    private SetupSubmitDataSourceFactory dataSourceFactory;

    private SetupInstallJdbcWriter jdbcWriter;

    private DatabaseExistenceProbe databaseExistenceProbe;

    private SetupPathTestService setupPathTestService;

    @BeforeEach
    void setUp() throws Exception
    {
        dataSourceFactory = new SetupSubmitDataSourceFactory()
        {
            @Override
            public com.alibaba.druid.pool.DruidDataSource createH2DataSource(SetupSubmitRequest request)
            {
                com.alibaba.druid.pool.DruidDataSource dataSource = super.createH2DataSource(request);
                String databaseName = DatabaseExistenceProbe.resolveH2DatabaseName(request);
                dataSource.setUrl(SetupTestSupport.h2JdbcUrl(workDir, databaseName));
                return dataSource;
            }
        };
        jdbcWriter = new SetupInstallJdbcWriter();
        databaseExistenceProbe = SetupTestSupport.probeWithDataDir(workDir, setupMysqlDatabaseService);
        setupPathTestService = new SetupPathTestService();

        ReflectionTestUtils.setField(setupInstallService, "setupSubmitDataSourceFactory", dataSourceFactory);
        ReflectionTestUtils.setField(setupInstallService, "setupInstallJdbcWriter", jdbcWriter);
        ReflectionTestUtils.setField(setupInstallService, "databaseExistenceProbe", databaseExistenceProbe);
        ReflectionTestUtils.setField(setupInstallService, "setupPathTestService", setupPathTestService);
        ReflectionTestUtils.setField(setupInstallService, "setupMigrationService", setupMigrationService);

        seedLegacyH2Database();
    }

    @Test
    void submit_existingH2_skipsMigrateAndUpdatesAdmin() throws Exception
    {
        SetupTestSupport.runWithMessages(() ->
        {
            try
            {
                when(installService.isInstalled()).thenReturn(false);
                when(installRuntimeActivator.activateAfterSetup(any(), any(DruidDataSource.class))).thenReturn(true);

                SetupSubmitRequest request = existingH2SubmitRequest();
                Map<String, Object> result = setupInstallService.submit(request);

                verify(setupMigrationService, never()).migrate(any(), any(DruidDataSource.class));
                verify(installApplicationRestarter, never()).scheduleRestartAfterSetup();
                assertFalse(Boolean.TRUE.equals(result.get("restartRequired")));
                assertFalse(Boolean.TRUE.equals(result.get("restarting")));
                assertTrue(result.containsKey("message"));

                SetupSubmitRequest verifyRequest = new SetupSubmitRequest();
                verifyRequest.setH2Database(DB_NAME);
                try (DruidDataSource dataSource = dataSourceFactory.createH2DataSource(verifyRequest))
                {
                    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
                    String username = jdbc.queryForObject(
                            "SELECT user_name FROM sys_user WHERE user_id = 1",
                            String.class);
                    String encrypted = jdbc.queryForObject(
                            "SELECT password FROM sys_user WHERE user_id = 1",
                            String.class);

                    assertEquals("setup_admin", username);
                    assertTrue(SecurityUtils.matchesPassword("setup_pass", encrypted));
                }
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
    }

    private SetupSubmitRequest existingH2SubmitRequest()
    {
        SetupSubmitRequest request = new SetupSubmitRequest();
        request.setDatabaseType("h2");
        request.setH2Database(DB_NAME);
        request.setDatabaseMode(DatabaseExistenceProbe.MODE_EXISTING);
        request.setCacheType("local");
        request.setProfile(workDir.resolve("uploadPath").toString());
        request.setTemp(workDir.resolve("uploadTemp").toString());
        request.setLogPath(workDir.resolve("logs").toString());
        request.setAiEnabled(false);
        request.setRegisterUser(false);
        request.setCaptchaEnabled(false);
        request.setAdminUsername("setup_admin");
        request.setAdminPassword("setup_pass");
        return request;
    }

    private void seedLegacyH2Database() throws Exception
    {
        SetupSubmitRequest seedRequest = new SetupSubmitRequest();
        seedRequest.setDatabaseType("h2");
        seedRequest.setH2Database(DB_NAME);

        SetupMigrationService migrationService = new SetupMigrationService();
        try (DruidDataSource dataSource = dataSourceFactory.createH2DataSource(seedRequest))
        {
            migrationService.migrate("h2", dataSource);
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.update(
                    "UPDATE sys_user SET user_name = ?, password = ? WHERE user_id = 1",
                    "legacy_admin",
                    SecurityUtils.encryptPassword("legacy_pass"));
        }
    }
}
