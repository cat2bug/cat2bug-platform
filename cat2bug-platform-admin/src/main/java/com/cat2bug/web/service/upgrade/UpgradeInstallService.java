package com.cat2bug.web.service.upgrade;

import com.cat2bug.common.config.InstallConfigExporter;
import com.cat2bug.common.config.InstallConfigSupport;
import com.cat2bug.common.config.InstallProperties;
import com.cat2bug.common.config.InstallTemplateLoader;
import com.cat2bug.common.config.UpgradeConfigMerger;
import com.cat2bug.common.config.UpgradeSupport;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.framework.service.InstallService;
import com.cat2bug.framework.service.UpgradeService;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import com.cat2bug.web.service.setup.SetupConfigWriter;
import com.cat2bug.web.service.setup.SetupMigrationService;
import com.cat2bug.web.service.setup.InstallApplicationRestarter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 升级提交：配置 merge 写入、Flyway 迁移、标记重启。
 */
@Service
public class UpgradeInstallService
{
    @Autowired
    private UpgradeService upgradeService;

    @Autowired
    private InstallService installService;

    @Autowired
    private InstallProperties installProperties;

    @Autowired
    private Environment environment;

    @Autowired
    private SetupConfigWriter setupConfigWriter;

    @Autowired
    private SetupMigrationService setupMigrationService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private InstallApplicationRestarter installApplicationRestarter;

    public Map<String, Object> submit(SetupSubmitRequest request, boolean retry)
    {
        if (UpgradeSupport.STATE_RESTART_REQUIRED.equals(upgradeService.resolveState()))
        {
            return scheduleRestartResult("升级已完成，应用正在自动重启…");
        }
        if (!upgradeService.isUpgradeRequired() && !retry)
        {
            throw new ServiceException("当前无需升级");
        }
        String lastStep = upgradeService.getLastStep();
        upgradeService.markRunning();
        try
        {
            if (shouldRunConfigStep(lastStep))
            {
                writeMergedConfig(request);
            }
            if (shouldRunMigrationStep(lastStep))
            {
                String databaseType = resolveDatabaseType(request);
                if (retry)
                {
                    setupMigrationService.repairIfChecksumConflict(databaseType, dataSource,
                            String.valueOf(upgradeService.getStatus().get("lastError")));
                }
                setupMigrationService.migrate(databaseType, dataSource);
            }
            finalizeInstallFile();
            installService.markCompleted();
            upgradeService.markRestartRequired();
            return scheduleRestartResult("升级完成，应用正在自动重启…");
        }
        catch (Exception e)
        {
            String step = shouldRunConfigStep(lastStep)
                    ? UpgradeSupport.STEP_CONFIG : InstallConfigSupport.UPGRADE_LAST_STEP_MIGRATION;
            upgradeService.markFailed(step, e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }

    private Map<String, Object> scheduleRestartResult(String message)
    {
        installApplicationRestarter.scheduleRestartAfterSetup();
        Map<String, Object> result = new HashMap<>(4);
        result.put("restartRequired", true);
        result.put("restarting", true);
        result.put("message", message);
        return result;
    }

    private boolean shouldRunConfigStep(String lastStep)
    {
        return StringUtils.isEmpty(lastStep) || UpgradeSupport.STEP_CONFIG.equals(lastStep);
    }

    private boolean shouldRunMigrationStep(String lastStep)
    {
        return StringUtils.isEmpty(lastStep)
                || UpgradeSupport.STEP_CONFIG.equals(lastStep)
                || InstallConfigSupport.UPGRADE_LAST_STEP_MIGRATION.equals(lastStep);
    }

    private void writeMergedConfig(SetupSubmitRequest request) throws Exception
    {
        String databaseType = resolveDatabaseType(request);
        Map<String, Object> base = InstallConfigExporter.exportFromEnvironment(environment);
        Map<String, Object> template = InstallTemplateLoader.loadTemplate(databaseType);
        Map<String, Object> wizardOverrides = setupConfigWriter.buildYamlRoot(request);
        InstallConfigSupport.setInstallCompleted(wizardOverrides, false);
        Map<String, Object> root = UpgradeConfigMerger.mergePreserveExisting(base, template, wizardOverrides);
        InstallConfigSupport.setInstallCompleted(root, false);
        Path target = installProperties.resolveConfigPath();
        InstallConfigSupport.writeInstallConfig(target, root);
        upgradeService.updateLastStep(InstallConfigSupport.UPGRADE_LAST_STEP_MIGRATION);
    }

    private void finalizeInstallFile() throws Exception
    {
        Path target = installProperties.resolveConfigPath();
        if (!InstallConfigSupport.isConfigFilePresent(installProperties.getConfigPath()))
        {
            throw new ServiceException("install 配置文件未生成");
        }
        Map<String, Object> root = InstallConfigSupport.loadYamlMap(target);
        if (root == null)
        {
            throw new ServiceException("无法读取 install 配置文件");
        }
        InstallConfigSupport.setInstallCompleted(root, true);
        InstallConfigSupport.writeInstallConfig(target, root);
    }

    private String resolveDatabaseType(SetupSubmitRequest request)
    {
        if (request != null && StringUtils.isNotEmpty(request.getDatabaseType()))
        {
            return request.getDatabaseType().toLowerCase();
        }
        return environment.getProperty("spring.database-type", "h2");
    }
}
