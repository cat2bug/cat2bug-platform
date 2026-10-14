package com.cat2bug.admin.setup;

import com.cat2bug.common.config.InstallConfigSupport;
import com.cat2bug.common.config.InstallTemplateLoader;
import com.cat2bug.common.config.UpgradeConfigMerger;
import com.cat2bug.common.config.UpgradeSupport;
import com.cat2bug.common.domain.setup.SetupSubmitRequest;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.service.InstallStateService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.sql.DataSource;
import com.cat2bug.common.setup.SetupMessages;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 升级提交：备份、配置 merge 写入、Flyway 迁移、标记重启。
 */
@ApplicationScoped
public class UpgradeInstallService {

    @Inject
    UpgradeService upgradeService;

    @Inject
    InstallStateService installStateService;

    @Inject
    SetupConfigWriter setupConfigWriter;

    @Inject
    SetupMigrationService setupMigrationService;

    @Inject
    UpgradeDatabaseBackupService upgradeDatabaseBackupService;

    @Inject
    InstallEffectiveDataSource effectiveDataSource;

    @Inject
    QuarkusInstallRestarter installRestarter;

    @Inject
    UpgradeAdminAuthService upgradeAdminAuthService;

    @Inject
    QuarkusInstallConfigExporter installConfigExporter;

    @ConfigProperty(name = "quarkus.mybatis.database-id", defaultValue = "h2")
    String databaseId;

    public Map<String, Object> submit(SetupSubmitRequest request, boolean retry) {
        if (UpgradeSupport.STATE_RESTART_REQUIRED.equals(upgradeService.resolveState())) {
            return scheduleRestartResult("升级已完成，应用正在自动重启…");
        }
        if (!upgradeService.isUpgradeRequired() && !retry) {
            throw new ApiException("当前无需升级");
        }
        upgradeAdminAuthService.verifyAdministrator(request);
        String step = upgradeService.getLastStep();
        upgradeService.markRunning();
        String runningStep = step;
        boolean configCompletedThisRun = false;
        try {
            if (shouldRunBackupStep(step, request)) {
                runningStep = UpgradeSupport.STEP_BACKUP;
                Path backupPath = upgradeDatabaseBackupService.backup(request, request.getBackupFileName());
                upgradeService.recordLastBackupFile(backupPath.toString());
                upgradeService.updateLastStep(UpgradeSupport.STEP_CONFIG);
                step = UpgradeSupport.STEP_CONFIG;
            }
            if (shouldRunConfigStep(step)) {
                runningStep = UpgradeSupport.STEP_CONFIG;
                if (shouldWriteConfig(request)) {
                    writeMergedConfig(request);
                    configCompletedThisRun = true;
                } else {
                    upgradeService.updateLastStep(InstallConfigSupport.UPGRADE_LAST_STEP_MIGRATION);
                }
                step = InstallConfigSupport.UPGRADE_LAST_STEP_MIGRATION;
            }
            if (configCompletedThisRun || shouldRunMigrationStep(step)) {
                runningStep = InstallConfigSupport.UPGRADE_LAST_STEP_MIGRATION;
                String databaseType = resolveDatabaseType(request);
                if (retry) {
                    setupMigrationService.repairIfChecksumConflict(
                            databaseType,
                            effectiveDataSource.get(),
                            String.valueOf(upgradeService.getStatus().get("lastError")));
                }
                setupMigrationService.migrate(databaseType, effectiveDataSource.get());
            }
            finalizeInstallFile();
            upgradeService.markRestartRequired();
            setupMigrationService.invalidatePendingMigrationCache();
            return scheduleRestartResult("升级完成，应用正在自动重启…");
        } catch (Exception e) {
            upgradeService.markFailed(runningStep, e.getMessage());
            if (e instanceof ApiException apiException) {
                throw apiException;
            }
            throw new ApiException(e.getMessage());
        }
    }

    public Map<String, Object> rollback(SetupSubmitRequest request) {
        upgradeAdminAuthService.verifyAdministrator(request);
        String backupPath = upgradeService.getLastBackupFile();
        if (StringUtils.isEmpty(backupPath)) {
            throw new ApiException(SetupMessages.msg("upgrade.rollback.no.backup"));
        }
        upgradeDatabaseBackupService.restore(request, Paths.get(backupPath));
        setupMigrationService.invalidatePendingMigrationCache();
        upgradeService.resetAfterRollback(SetupMessages.msg("upgrade.rollback.completed"));
        Map<String, Object> result = new HashMap<>(4);
        result.put("message", SetupMessages.msg("upgrade.rollback.completed"));
        result.put("lastBackupFile", backupPath);
        return result;
    }

    private Map<String, Object> scheduleRestartResult(String message) {
        installRestarter.scheduleRestartAfterSetup();
        Map<String, Object> result = new HashMap<>(4);
        result.put("restartRequired", true);
        result.put("restarting", true);
        result.put("message", message);
        return result;
    }

    private boolean isBackupEnabled(SetupSubmitRequest request) {
        return request == null || request.getBackupEnabled() == null || Boolean.TRUE.equals(request.getBackupEnabled());
    }

    private boolean shouldRunBackupStep(String lastStep, SetupSubmitRequest request) {
        if (!isBackupEnabled(request)) {
            return false;
        }
        return StringUtils.isEmpty(lastStep) || UpgradeSupport.STEP_BACKUP.equals(lastStep);
    }

    private boolean shouldRunConfigStep(String lastStep) {
        return StringUtils.isEmpty(lastStep)
                || UpgradeSupport.STEP_BACKUP.equals(lastStep)
                || UpgradeSupport.STEP_CONFIG.equals(lastStep);
    }

    private boolean shouldWriteConfig(SetupSubmitRequest request) {
        if (isApplyConfigChanges(request)) {
            return true;
        }
        return !InstallConfigSupport.isConfigFilePresent(installStateService.getConfigPath());
    }

    private static boolean isApplyConfigChanges(SetupSubmitRequest request) {
        if (request == null || request.getApplyConfigChanges() == null) {
            return true;
        }
        return Boolean.TRUE.equals(request.getApplyConfigChanges());
    }

    private boolean shouldRunMigrationStep(String lastStep) {
        return InstallConfigSupport.UPGRADE_LAST_STEP_MIGRATION.equals(lastStep);
    }

    private void writeMergedConfig(SetupSubmitRequest request) throws Exception {
        String databaseType = resolveDatabaseType(request);
        boolean installCompleted = InstallConfigSupport.isInstallCompletedOnDisk(installStateService.getConfigPath());
        Map<String, Object> base = InstallConfigSupport.loadYamlMap(
                InstallConfigSupport.resolveConfigPath(installStateService.getConfigPath()));
        if (base == null || base.isEmpty()) {
            base = installConfigExporter.exportFromRuntime();
        }
        Map<String, Object> template = InstallTemplateLoader.loadTemplate(databaseType);
        Map<String, Object> wizardOverrides = setupConfigWriter.buildYamlRoot(request);
        InstallConfigSupport.setInstallCompleted(wizardOverrides, installCompleted);
        Map<String, Object> root = UpgradeConfigMerger.mergePreserveExisting(base, template, wizardOverrides);
        InstallConfigSupport.setInstallCompleted(root, installCompleted);
        Path target = InstallConfigSupport.resolveWorkingDirectoryConfigPath(installStateService.getConfigPath());
        InstallConfigSupport.writeInstallConfig(target, root);
        upgradeService.updateLastStep(InstallConfigSupport.UPGRADE_LAST_STEP_MIGRATION);
    }

    private void finalizeInstallFile() throws Exception {
        if (!InstallConfigSupport.isConfigFilePresent(installStateService.getConfigPath())) {
            throw new ApiException("install 配置文件未生成");
        }
        Path target = InstallConfigSupport.resolveConfigPath(installStateService.getConfigPath());
        Map<String, Object> root = InstallConfigSupport.loadYamlMap(target);
        if (root == null) {
            throw new ApiException("无法读取 install 配置文件");
        }
        InstallConfigSupport.setInstallCompleted(root, true);
        InstallConfigSupport.writeInstallConfig(target, root);
    }

    private String resolveDatabaseType(SetupSubmitRequest request) {
        if (request != null && StringUtils.isNotEmpty(request.getDatabaseType())) {
            return request.getDatabaseType().toLowerCase();
        }
        return "mysql".equalsIgnoreCase(databaseId) ? "mysql" : "h2";
    }
}
