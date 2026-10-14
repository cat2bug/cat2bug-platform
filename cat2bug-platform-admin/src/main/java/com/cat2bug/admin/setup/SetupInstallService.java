package com.cat2bug.admin.setup;

import com.cat2bug.common.domain.setup.SetupPathTestRequest;
import com.cat2bug.common.domain.setup.SetupRedisTestRequest;
import com.cat2bug.common.domain.setup.SetupSubmitRequest;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.setup.SetupMessages;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.service.InstallStateService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class SetupInstallService {

    @Inject
    InstallStateService installStateService;

    @Inject
    SetupConfigWriter setupConfigWriter;

    @Inject
    SetupMigrationService setupMigrationService;

    @Inject
    SetupSubmitDataSourceFactory setupSubmitDataSourceFactory;

    @Inject
    SetupInstallJdbcWriter setupInstallJdbcWriter;

    @Inject
    SetupPathTestService setupPathTestService;

    @Inject
    SetupRedisTestService setupRedisTestService;

    @Inject
    SetupMysqlDatabaseService setupMysqlDatabaseService;

    @Inject
    DatabaseExistenceProbe databaseExistenceProbe;

    @Inject
    QuarkusInstallRuntimeActivator installRuntimeActivator;

    public Map<String, Object> submit(SetupSubmitRequest request) throws Exception {
        if (installStateService.isInstalled()) {
            throw new ApiException(SetupMessages.msg("setup.install.already"));
        }
        validateSubmit(request);

        String databaseType = request.getDatabaseType().toLowerCase();
        String mysqlWarning = null;
        SimpleDataSource setupDataSource = null;
        try {
            if (SetupSubmitDataSourceFactory.isMysql(request)) {
                MysqlInstallResult mysqlInstall = applyMysqlInstall(request);
                setupDataSource = mysqlInstall.dataSource();
                mysqlWarning = mysqlInstall.warningMessage();
            } else {
                setupDataSource = applyH2Install(request, databaseType);
            }

            setupConfigWriter.write(request);
            boolean restartRequired = installRuntimeActivator.activateAfterSetup(request);

            Map<String, Object> result = new HashMap<>(5);
            result.put("restartRequired", restartRequired);
            result.put("restarting", restartRequired);
            String message = SetupMessages.msg("setup.install.completed");
            if (StringUtils.isNotEmpty(mysqlWarning)) {
                result.put("mysqlManualImportHint", mysqlWarning);
                message = message + "。" + mysqlWarning;
            }
            result.put("message", message);
            return result;
        } finally {
            if (setupDataSource != null) {
                setupDataSource.close();
            }
        }
    }

    private MysqlInstallResult applyMysqlInstall(SetupSubmitRequest request) {
        boolean isNew = DatabaseExistenceProbe.MODE_NEW.equals(request.getDatabaseMode());
        String warning = null;
        if (isNew) {
            SetupMysqlDatabaseService.MysqlDatabasePrepareResult prepareResult = setupMysqlDatabaseService.prepare(request);
            warning = prepareResult.warningMessage();
        }
        SimpleDataSource mysqlDataSource = setupSubmitDataSourceFactory.createMysqlDataSource(request);
        if (isNew) {
            if (!setupInstallJdbcWriter.isSchemaPresent(mysqlDataSource)) {
                setupMigrationService.migrate("mysql", mysqlDataSource);
            }
            if (!setupInstallJdbcWriter.isSchemaPresent(mysqlDataSource)) {
                throw new ApiException(SetupMessages.msg("setup.install.schema.missing"));
            }
        } else if (!setupInstallJdbcWriter.isSchemaPresent(mysqlDataSource)) {
            throw new ApiException(SetupMessages.msg("setup.install.schema.missing"));
        }
        setupInstallJdbcWriter.createOrUpdateAdmin(mysqlDataSource, request);
        writeAccountConfigs(mysqlDataSource, request);
        setupInstallJdbcWriter.markInstallCompleted(mysqlDataSource);
        return new MysqlInstallResult(mysqlDataSource, warning);
    }

    private record MysqlInstallResult(SimpleDataSource dataSource, String warningMessage) {
    }

    private SimpleDataSource applyH2Install(SetupSubmitRequest request, String databaseType) {
        SimpleDataSource h2DataSource = setupSubmitDataSourceFactory.createH2DataSource(request);
        boolean schemaPresent = setupInstallJdbcWriter.isSchemaPresent(h2DataSource);
        boolean attachExisting = DatabaseExistenceProbe.MODE_EXISTING.equals(request.getDatabaseMode());
        if (attachExisting && !schemaPresent) {
            throw new ApiException(SetupMessages.msg("setup.install.h2.existing.empty"));
        }
        if (!attachExisting) {
            if (!schemaPresent) {
                setupMigrationService.migrate(databaseType, h2DataSource);
            }
            if (!setupInstallJdbcWriter.isSchemaPresent(h2DataSource)) {
                throw new ApiException(SetupMessages.msg("setup.install.schema.missing"));
            }
        }
        setupInstallJdbcWriter.createOrUpdateAdmin(h2DataSource, request);
        writeAccountConfigs(h2DataSource, request);
        setupInstallJdbcWriter.markInstallCompleted(h2DataSource);
        return h2DataSource;
    }

    private void writeAccountConfigs(SimpleDataSource dataSource, SetupSubmitRequest request) {
        setupInstallJdbcWriter.upsertConfig(dataSource, "sys.account.registerUser",
                Boolean.TRUE.equals(request.getRegisterUser()) ? "true" : "false",
                "账号自助-是否开启用户注册功能");
        setupInstallJdbcWriter.upsertConfig(dataSource, "sys.account.captchaEnabled",
                Boolean.TRUE.equals(request.getCaptchaEnabled()) ? "true" : "false",
                "登录验证码");
    }

    private void validateSubmit(SetupSubmitRequest request) {
        if (request == null) {
            throw new ApiException(SetupMessages.msg("setup.install.params.empty"));
        }
        if (StringUtils.isEmpty(request.getDatabaseType())) {
            throw new ApiException(SetupMessages.msg("setup.install.db.type.required"));
        }
        String dbType = request.getDatabaseType().toLowerCase();
        if (!"h2".equals(dbType) && !"mysql".equals(dbType)) {
            throw new ApiException(SetupMessages.msg("setup.install.db.type.unsupported"));
        }
        if ("mysql".equals(dbType)) {
            if (StringUtils.isEmpty(request.getMysqlHost()) || request.getMysqlPort() == null
                    || StringUtils.isEmpty(request.getMysqlDatabase())
                    || StringUtils.isEmpty(request.getMysqlUsername())) {
                throw new ApiException(SetupMessages.msg("setup.install.mysql.incomplete"));
            }
            DatabaseExistenceProbe.assertValidDatabaseName(request.getMysqlDatabase());
        } else {
            DatabaseExistenceProbe.assertValidDatabaseName(DatabaseExistenceProbe.resolveH2DatabaseName(request));
        }
        if (StringUtils.isEmpty(request.getDatabaseMode())) {
            throw new ApiException(SetupMessages.msg("setup.install.database.mode.required"));
        }
        String databaseMode = request.getDatabaseMode().trim().toLowerCase();
        DatabaseExistenceProbe.assertValidDatabaseMode(databaseMode);
        request.setDatabaseMode(databaseMode);
        String probedMode = databaseExistenceProbe.probeMode(request);
        if (!databaseMode.equals(probedMode)) {
            throw new ApiException(SetupMessages.msg("setup.install.database.mode.mismatch"));
        }
        String cacheType = StringUtils.isNotEmpty(request.getCacheType())
                ? request.getCacheType().toLowerCase() : "local";
        if (!"local".equals(cacheType) && !"redis".equals(cacheType)) {
            throw new ApiException(SetupMessages.msg("setup.install.cache.unsupported"));
        }
        if ("redis".equals(cacheType)) {
            if (StringUtils.isEmpty(request.getRedisHost()) || request.getRedisPort() == null) {
                throw new ApiException(SetupMessages.msg("setup.install.redis.incomplete"));
            }
            Map<String, Object> redisTest = setupRedisTestService.test(toRedisTest(request));
            if (!Boolean.TRUE.equals(redisTest.get("success"))) {
                throw new ApiException(SetupMessages.msg("setup.install.redis.test.failed", redisTest.get("message")));
            }
        }
        applySubmitDefaults(request);
        assertPathWritable(request.getProfile(), SetupMessages.msg("setup.install.path.label.profile"));
        assertPathWritable(request.getTemp(), SetupMessages.msg("setup.install.path.label.temp"));
        if (StringUtils.isNotEmpty(request.getLogPath())) {
            assertPathWritable(request.getLogPath(), SetupMessages.msg("setup.install.path.label.log"));
        }
        if (StringUtils.isEmpty(request.getAdminUsername())) {
            request.setAdminUsername("admin");
        }
        if (StringUtils.isEmpty(request.getAdminPassword())) {
            request.setAdminPassword("cat2bug");
        }
        if (Boolean.TRUE.equals(request.getAiEnabled()) && StringUtils.isEmpty(request.getAiHost())) {
            throw new ApiException(SetupMessages.msg("setup.install.ai.host.required"));
        }
    }

    private void applySubmitDefaults(SetupSubmitRequest request) {
        if (StringUtils.isEmpty(request.getProfile())) {
            request.setProfile("uploadPath");
        }
        if (StringUtils.isEmpty(request.getTemp())) {
            request.setTemp(resolveDefaultTemp(request.getProfile()));
        }
    }

    private static String resolveDefaultTemp(String profile) {
        String p = profile.trim();
        if (p.endsWith("Path")) {
            return p.substring(0, p.length() - 4) + "Temp";
        }
        if (p.endsWith("/") || p.endsWith("\\")) {
            return p + "temp";
        }
        return p + "Temp";
    }

    private void assertPathWritable(String path, String label) {
        SetupPathTestRequest test = new SetupPathTestRequest();
        test.setPath(path);
        Map<String, Object> result = setupPathTestService.test(test);
        if (!Boolean.TRUE.equals(result.get("success"))) {
            throw new ApiException(SetupMessages.msg("setup.install.path.unavailable", label, result.get("message")));
        }
    }

    private static SetupRedisTestRequest toRedisTest(SetupSubmitRequest request) {
        SetupRedisTestRequest redis = new SetupRedisTestRequest();
        redis.setHost(request.getRedisHost());
        redis.setPort(request.getRedisPort());
        redis.setPassword(request.getRedisPassword());
        redis.setDatabase(request.getRedisDatabase());
        return redis;
    }
}
