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
import com.cat2bug.web.service.setup.SetupMessages;
import com.cat2bug.web.service.setup.SetupMigrationService;
import com.cat2bug.web.service.setup.InstallApplicationRestarter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 升级提交：备份、配置 merge 写入、Flyway 迁移、标记重启。
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
    private UpgradeDatabaseBackupService upgradeDatabaseBackupService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private InstallApplicationRestarter installApplicationRestarter;

    @Autowired
    private UpgradeAdminAuthService upgradeAdminAuthService;

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
        upgradeAdminAuthService.verifyAdministrator(request);
        String step = upgradeService.getLastStep();
        upgradeService.markRunning();
        String runningStep = step;
        boolean configCompletedThisRun = false;
        try
        {
            if (shouldRunBackupStep(step, request))
            {
                runningStep = UpgradeSupport.STEP_BACKUP;
                Path backupPath = upgradeDatabaseBackupService.backup(request, request.getBackupFileName());
                upgradeService.recordLastBackupFile(backupPath.toString());
                upgradeService.updateLastStep(UpgradeSupport.STEP_CONFIG);
                step = UpgradeSupport.STEP_CONFIG;
            }
            if (shouldRunConfigStep(step))
            {
                runningStep = UpgradeSupport.STEP_CONFIG;
                if (shouldWriteConfig(request))
                {
                    writeMergedConfig(request);
                    configCompletedThisRun = true;
                }
                else
                {
                    upgradeService.updateLastStep(InstallConfigSupport.UPGRADE_LAST_STEP_MIGRATION);
                }
                step = InstallConfigSupport.UPGRADE_LAST_STEP_MIGRATION;
            }
            if (configCompletedThisRun || shouldRunMigrationStep(step))
            {
                runningStep = InstallConfigSupport.UPGRADE_LAST_STEP_MIGRATION;
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
            upgradeService.markFailed(runningStep, e.getMessage());
            throw new ServiceException(e.getMessage());
        }
    }

    public Map<String, Object> rollback(SetupSubmitRequest request)
    {
        upgradeAdminAuthService.verifyAdministrator(request);
        String backupPath = upgradeService.getLastBackupFile();
        if (StringUtils.isEmpty(backupPath))
        {
            throw new ServiceException(SetupMessages.msg("upgrade.rollback.no.backup"));
        }
        upgradeDatabaseBackupService.restore(request, Paths.get(backupPath));
        setupMigrationService.invalidatePendingMigrationCache();
        upgradeService.resetAfterRollback(SetupMessages.msg("upgrade.rollback.completed"));
        Map<String, Object> result = new HashMap<>(4);
        result.put("message", SetupMessages.msg("upgrade.rollback.completed"));
        result.put("lastBackupFile", backupPath);
        return result;
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

    private boolean isBackupEnabled(SetupSubmitRequest request)
    {
        return request == null || request.getBackupEnabled() == null || Boolean.TRUE.equals(request.getBackupEnabled());
    }

    private boolean shouldRunBackupStep(String lastStep, SetupSubmitRequest request)
    {
        if (!isBackupEnabled(request))
        {
            return false;
        }
        return StringUtils.isEmpty(lastStep) || UpgradeSupport.STEP_BACKUP.equals(lastStep);
    }

    private boolean shouldRunConfigStep(String lastStep)
    {
        return StringUtils.isEmpty(lastStep)
                || UpgradeSupport.STEP_BACKUP.equals(lastStep)
                || UpgradeSupport.STEP_CONFIG.equals(lastStep);
    }

    private boolean shouldWriteConfig(SetupSubmitRequest request)
    {
        if (isApplyConfigChanges(request))
        {
            return true;
        }
        return !InstallConfigSupport.isConfigFilePresent(installProperties.getConfigPath());
    }

    private static boolean isApplyConfigChanges(SetupSubmitRequest request)
    {
        if (request == null || request.getApplyConfigChanges() == null)
        {
            return true;
        }
        return Boolean.TRUE.equals(request.getApplyConfigChanges());
    }

    private boolean shouldRunMigrationStep(String lastStep)
    {
        return InstallConfigSupport.UPGRADE_LAST_STEP_MIGRATION.equals(lastStep);
    }

    private void writeMergedConfig(SetupSubmitRequest request) throws Exception
    {
        String databaseType = resolveDatabaseType(request);
        boolean installCompleted = installProperties.isInstallCompletedOnDisk();
        Map<String, Object> base = InstallConfigSupport.loadYamlMap(installProperties.resolveConfigPath());
        if (base == null || base.isEmpty())
        {
            base = InstallConfigExporter.exportFromEnvironment(environment);
        }
        Map<String, Object> template = InstallTemplateLoader.loadTemplate(databaseType);
        Map<String, Object> wizardOverrides = setupConfigWriter.buildYamlRoot(request);
        InstallConfigSupport.setInstallCompleted(wizardOverrides, installCompleted);
        Map<String, Object> root = UpgradeConfigMerger.mergePreserveExisting(base, template, wizardOverrides);
        InstallConfigSupport.setInstallCompleted(root, installCompleted);
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
