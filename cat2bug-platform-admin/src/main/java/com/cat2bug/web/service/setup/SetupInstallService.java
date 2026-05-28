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

import javax.sql.DataSource;
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
    private DataSource dataSource;

    @Autowired
    private SetupPathTestService setupPathTestService;

    @Autowired
    private SetupRedisTestService setupRedisTestService;

    @Autowired
    private SetupMysqlDatabaseService setupMysqlDatabaseService;

    @Autowired
    private ISysConfigService configService;

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
        if (SetupSubmitDataSourceFactory.isMysql(request))
        {
            mysqlWarning = applyMysqlInstall(request);
        }
        else
        {
            applyH2Install(request, databaseType);
        }

        setupConfigWriter.write(request);

        safeResetConfigCache();

        Map<String, Object> result = new HashMap<>(4);
        result.put("restartRequired", true);
        String message = SetupMessages.msg("setup.install.completed");
        if (StringUtils.isNotEmpty(mysqlWarning))
        {
            result.put("mysqlManualImportHint", mysqlWarning);
            message = message + "。" + mysqlWarning;
        }
        result.put("message", message);
        return result;
    }

    private String applyMysqlInstall(SetupSubmitRequest request)
    {
        SetupMysqlDatabaseService.MysqlDatabasePrepareResult prepareResult = setupMysqlDatabaseService.prepare(request);
        DruidDataSource mysqlDataSource = setupSubmitDataSourceFactory.createMysqlDataSource(request);
        try
        {
            if (!setupInstallJdbcWriter.isSchemaPresent(mysqlDataSource))
            {
                setupMigrationService.migrate("mysql", mysqlDataSource);
            }
            if (!setupInstallJdbcWriter.isSchemaPresent(mysqlDataSource))
            {
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
            return prepareResult.warningMessage();
        }
        finally
        {
            mysqlDataSource.close();
        }
    }

    private void applyH2Install(SetupSubmitRequest request, String databaseType)
    {
        if (!installService.isSchemaPresent())
        {
            setupMigrationService.migrate(databaseType, dataSource);
        }
        setupInstallJdbcWriter.createOrUpdateAdmin(dataSource, request);
        setupInstallJdbcWriter.upsertConfig(dataSource, "sys.account.registerUser",
                Boolean.TRUE.equals(request.getRegisterUser()) ? "true" : "false",
                "账号自助-是否开启用户注册功能");
        setupInstallJdbcWriter.upsertConfig(dataSource, "sys.account.captchaEnabled",
                Boolean.TRUE.equals(request.getCaptchaEnabled()) ? "true" : "false",
                "登录验证码");
        setupInstallJdbcWriter.markInstallCompleted(dataSource);
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
            SetupMysqlDatabaseService.assertValidDatabaseName(request.getMysqlDatabase());
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
