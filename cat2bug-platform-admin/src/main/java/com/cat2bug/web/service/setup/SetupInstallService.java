package com.cat2bug.web.service.setup;

import com.alibaba.druid.pool.DruidDataSource;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.framework.service.InstallService;
import com.cat2bug.system.service.ISysConfigService;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 安装提交：写配置、迁移、创建管理员、写入 sys_config
 */
@Service
public class SetupInstallService
{
    @Autowired
    private InstallService installService;

    @Autowired
    private SetupConfigWriter setupConfigWriter;

    @Autowired
    private SetupMigrationService setupMigrationService;

    @Autowired
    private SetupSubmitDataSourceFactory setupSubmitDataSourceFactory;

    @Autowired
    private SetupInstallJdbcWriter setupInstallJdbcWriter;

    @Autowired
    private SetupPathTestService setupPathTestService;

    @Autowired
    private SetupRedisTestService setupRedisTestService;

    @Autowired
    private SetupMysqlDatabaseService setupMysqlDatabaseService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private InstallApplicationRestarter installApplicationRestarter;

    @Autowired
    private DatabaseExistenceProbe databaseExistenceProbe;

    @Autowired
    private InstallRuntimeActivator installRuntimeActivator;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submit(SetupSubmitRequest request) throws Exception
    {
        if (installService.isInstalled())
        {
            throw new ServiceException(SetupMessages.msg("setup.install.already"));
        }
        validateSubmit(request);

        String databaseType = request.getDatabaseType().toLowerCase();
        String mysqlWarning = null;
        DruidDataSource setupDataSource = null;
        boolean runtimeReady = false;
        try
        {
            if (SetupSubmitDataSourceFactory.isMysql(request))
            {
                MysqlInstallResult mysqlInstall = applyMysqlInstall(request);
                setupDataSource = mysqlInstall.dataSource();
                mysqlWarning = mysqlInstall.warningMessage();
            }
            else
            {
                setupDataSource = applyH2Install(request, databaseType);
            }

            setupConfigWriter.write(request);
            setupMigrationService.invalidatePendingMigrationCache();
            safeResetConfigCache();

            runtimeReady = installRuntimeActivator.activateAfterSetup(request, setupDataSource);
            if (!runtimeReady)
            {
                installApplicationRestarter.scheduleRestartAfterSetup();
            }

            Map<String, Object> result = new HashMap<>(5);
            result.put("restartRequired", !runtimeReady);
            result.put("restarting", !runtimeReady);
            String message = SetupMessages.msg("setup.install.completed");
            if (StringUtils.isNotEmpty(mysqlWarning))
            {
                result.put("mysqlManualImportHint", mysqlWarning);
                message = message + "。" + mysqlWarning;
            }
            result.put("message", message);
            return result;
        }
        finally
        {
            if (setupDataSource != null && !runtimeReady)
            {
                setupDataSource.close();
            }
        }
    }

    private MysqlInstallResult applyMysqlInstall(SetupSubmitRequest request)
    {
        boolean isNew = DatabaseExistenceProbe.MODE_NEW.equals(request.getDatabaseMode());
        String warning = null;
        if (isNew)
        {
            SetupMysqlDatabaseService.MysqlDatabasePrepareResult prepareResult = setupMysqlDatabaseService.prepare(request);
            warning = prepareResult.warningMessage();
        }
        DruidDataSource mysqlDataSource = setupSubmitDataSourceFactory.createMysqlDataSource(request);
        if (isNew)
        {
            if (!setupInstallJdbcWriter.isSchemaPresent(mysqlDataSource))
            {
                setupMigrationService.migrate("mysql", mysqlDataSource);
            }
            if (!setupInstallJdbcWriter.isSchemaPresent(mysqlDataSource))
            {
                mysqlDataSource.close();
                throw new ServiceException(SetupMessages.msg("setup.install.schema.missing"));
            }
        }
        else if (!setupInstallJdbcWriter.isSchemaPresent(mysqlDataSource))
        {
            mysqlDataSource.close();
            throw new ServiceException(SetupMessages.msg("setup.install.schema.missing"));
        }
        setupInstallJdbcWriter.createOrUpdateAdmin(mysqlDataSource, request);
        setupInstallJdbcWriter.upsertConfig(mysqlDataSource, "sys.account.registerUser",
                Boolean.TRUE.equals(request.getRegisterUser()) ? "true" : "false",
                "账号自助-是否开启用户注册功能");
        setupInstallJdbcWriter.upsertConfig(mysqlDataSource, "sys.account.captchaEnabled",
                Boolean.TRUE.equals(request.getCaptchaEnabled()) ? "true" : "false",
                "登录验证码");
        setupInstallJdbcWriter.markInstallCompleted(mysqlDataSource);
        return new MysqlInstallResult(mysqlDataSource, warning);
    }

    private record MysqlInstallResult(DruidDataSource dataSource, String warningMessage)
    {
    }

    private DruidDataSource applyH2Install(SetupSubmitRequest request, String databaseType)
    {
        DruidDataSource h2DataSource = setupSubmitDataSourceFactory.createH2DataSource(request);
        boolean schemaPresent = setupInstallJdbcWriter.isSchemaPresent(h2DataSource);
        boolean attachExisting = DatabaseExistenceProbe.MODE_EXISTING.equals(request.getDatabaseMode());
        if (attachExisting && !schemaPresent)
        {
            h2DataSource.close();
            throw new ServiceException(SetupMessages.msg("setup.install.h2.existing.empty"));
        }
        if (!attachExisting)
        {
            if (!schemaPresent)
            {
                setupMigrationService.migrate(databaseType, h2DataSource);
            }
            if (!setupInstallJdbcWriter.isSchemaPresent(h2DataSource))
            {
                h2DataSource.close();
                throw new ServiceException(SetupMessages.msg("setup.install.schema.missing"));
            }
        }
        setupInstallJdbcWriter.createOrUpdateAdmin(h2DataSource, request);
        setupInstallJdbcWriter.upsertConfig(h2DataSource, "sys.account.registerUser",
                Boolean.TRUE.equals(request.getRegisterUser()) ? "true" : "false",
                "账号自助-是否开启用户注册功能");
        setupInstallJdbcWriter.upsertConfig(h2DataSource, "sys.account.captchaEnabled",
                Boolean.TRUE.equals(request.getCaptchaEnabled()) ? "true" : "false",
                "登录验证码");
        setupInstallJdbcWriter.markInstallCompleted(h2DataSource);
        return h2DataSource;
    }

    private void safeResetConfigCache()
    {
        try
        {
            configService.resetConfigCache();
        }
        catch (Exception e)
        {
            // 引导模式下运行时库可能尚未写入配置，忽略缓存刷新失败
        }
    }

    private void validateSubmit(SetupSubmitRequest request)
    {
        if (request == null)
        {
            throw new ServiceException(SetupMessages.msg("setup.install.params.empty"));
        }
        if (StringUtils.isEmpty(request.getDatabaseType()))
        {
            throw new ServiceException(SetupMessages.msg("setup.install.db.type.required"));
        }
        String dbType = request.getDatabaseType().toLowerCase();
        if (!"h2".equals(dbType) && !"mysql".equals(dbType))
        {
            throw new ServiceException(SetupMessages.msg("setup.install.db.type.unsupported"));
        }
        if ("mysql".equals(dbType))
        {
            if (StringUtils.isEmpty(request.getMysqlHost()) || request.getMysqlPort() == null
                    || StringUtils.isEmpty(request.getMysqlDatabase())
                    || StringUtils.isEmpty(request.getMysqlUsername()))
            {
                throw new ServiceException(SetupMessages.msg("setup.install.mysql.incomplete"));
            }
            DatabaseExistenceProbe.assertValidDatabaseName(request.getMysqlDatabase());
        }
        else
        {
            DatabaseExistenceProbe.assertValidDatabaseName(DatabaseExistenceProbe.resolveH2DatabaseName(request));
        }
        if (StringUtils.isEmpty(request.getDatabaseMode()))
        {
            throw new ServiceException(SetupMessages.msg("setup.install.database.mode.required"));
        }
        String databaseMode = request.getDatabaseMode().trim().toLowerCase();
        DatabaseExistenceProbe.assertValidDatabaseMode(databaseMode);
        request.setDatabaseMode(databaseMode);
        String probedMode = databaseExistenceProbe.probeMode(request);
        if (!databaseMode.equals(probedMode))
        {
            throw new ServiceException(SetupMessages.msg("setup.install.database.mode.mismatch"));
        }
        String cacheType = StringUtils.isNotEmpty(request.getCacheType())
                ? request.getCacheType().toLowerCase() : "local";
        if (!"local".equals(cacheType) && !"redis".equals(cacheType))
        {
            throw new ServiceException(SetupMessages.msg("setup.install.cache.unsupported"));
        }
        if ("redis".equals(cacheType))
        {
            if (StringUtils.isEmpty(request.getRedisHost()) || request.getRedisPort() == null)
            {
                throw new ServiceException(SetupMessages.msg("setup.install.redis.incomplete"));
            }
            Map<String, Object> redisTest = setupRedisTestService.test(toRedisTest(request));
            if (!Boolean.TRUE.equals(redisTest.get("success")))
            {
                throw new ServiceException(SetupMessages.msg("setup.install.redis.test.failed", redisTest.get("message")));
            }
        }
        applySubmitDefaults(request);
        assertPathWritable(request.getProfile(), SetupMessages.msg("setup.install.path.label.profile"));
        assertPathWritable(request.getTemp(), SetupMessages.msg("setup.install.path.label.temp"));
        if (StringUtils.isNotEmpty(request.getLogPath()))
        {
            assertPathWritable(request.getLogPath(), SetupMessages.msg("setup.install.path.label.log"));
        }
        if (StringUtils.isEmpty(request.getAdminUsername()))
        {
            request.setAdminUsername("admin");
        }
        if (StringUtils.isEmpty(request.getAdminPassword()))
        {
            request.setAdminPassword("cat2bug");
        }
        if (Boolean.TRUE.equals(request.getAiEnabled()) && StringUtils.isEmpty(request.getAiHost()))
        {
            throw new ServiceException(SetupMessages.msg("setup.install.ai.host.required"));
        }
    }

    /**
     * 补全向导未单独采集的字段（如临时目录），须在路径校验之前调用。
     */
    private void applySubmitDefaults(SetupSubmitRequest request)
    {
        if (StringUtils.isEmpty(request.getProfile()))
        {
            request.setProfile("uploadPath");
        }
        if (StringUtils.isEmpty(request.getTemp()))
        {
            request.setTemp(resolveDefaultTemp(request.getProfile()));
        }
    }

    /**
     * 与 {@code application.yml} 中 uploadPath / uploadTemp 命名约定一致。
     */
    private static String resolveDefaultTemp(String profile)
    {
        String p = profile.trim();
        if (p.endsWith("Path"))
        {
            return p.substring(0, p.length() - 4) + "Temp";
        }
        if (p.endsWith("/") || p.endsWith("\\"))
        {
            return p + "temp";
        }
        return p + "Temp";
    }

    private void assertPathWritable(String path, String label)
    {
        com.cat2bug.web.domain.setup.SetupPathTestRequest test = new com.cat2bug.web.domain.setup.SetupPathTestRequest();
        test.setPath(path);
        Map<String, Object> result = setupPathTestService.test(test);
        if (!Boolean.TRUE.equals(result.get("success")))
        {
            throw new ServiceException(SetupMessages.msg("setup.install.path.unavailable", label, result.get("message")));
        }
    }

    private static com.cat2bug.web.domain.setup.SetupRedisTestRequest toRedisTest(SetupSubmitRequest request)
    {
        com.cat2bug.web.domain.setup.SetupRedisTestRequest redis = new com.cat2bug.web.domain.setup.SetupRedisTestRequest();
        redis.setHost(request.getRedisHost());
        redis.setPort(request.getRedisPort());
        redis.setPassword(request.getRedisPassword());
        redis.setDatabase(request.getRedisDatabase());
        return redis;
    }
}
