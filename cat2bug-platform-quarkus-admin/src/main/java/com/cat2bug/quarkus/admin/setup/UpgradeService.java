package com.cat2bug.quarkus.admin.setup;

import com.cat2bug.quarkus.common.config.InstallConfigSupport;
import com.cat2bug.quarkus.common.config.UpgradeSupport;
import com.cat2bug.quarkus.common.util.StringUtils;
import com.cat2bug.quarkus.system.service.InstallStateService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Legacy 升级向导状态与触发条件。
 */
@ApplicationScoped
public class UpgradeService {

    private static final Logger log = Logger.getLogger(UpgradeService.class);

    @ConfigProperty(name = "cat2bug.upgrade.skip", defaultValue = "false")
    String upgradeSkipProperty;

    @ConfigProperty(name = "cat2bug.upgrade.state-path", defaultValue = "")
    java.util.Optional<String> upgradeStatePath;

    @ConfigProperty(name = "cat2bug.version", defaultValue = "1.0.0")
    String version;

    @ConfigProperty(name = "quarkus.mybatis.database-id", defaultValue = "h2")
    String databaseId;

    @Inject
    InstallStateService installStateService;

    @Inject
    SetupMigrationService setupMigrationService;

    private volatile String cachedState = InstallConfigSupport.UPGRADE_STATE_COMPLETED;
    private volatile String cachedLastError = "";
    private volatile String cachedLastStep = "";
    private volatile int cachedAttemptCount;
    private volatile String cachedTargetVersion = "";
    private volatile String cachedCompletedVersion = "";

    public void onStartup() {
        if (isUpgradeSkipped()) {
            return;
        }
        syncFromDisk();
        resetStaleUpgradeStateWhenInstallIncomplete();
        syncPendingUpgradeStateWhenSchemaDrift();
        syncCompletedUpgradeStateWhenNoWorkNeeded();
        if (UpgradeSupport.STATE_RESTART_REQUIRED.equals(readRawState())) {
            Map<String, Object> section = readSection();
            section.put("state", UpgradeSupport.STATE_COMPLETED);
            section.put("completedVersion", version);
            section.put("lastError", "");
            section.put("lastStep", "");
            writeSection(section);
            markInstallCompletedOnDiskIfNeeded();
            log.info("升级重启完成，状态已转为 completed");
        }
    }

    public boolean isUpgradeSkipped() {
        return "true".equalsIgnoreCase(upgradeSkipProperty)
                || "true".equalsIgnoreCase(System.getenv("CAT2BUG_UPGRADE_SKIP"));
    }

    public boolean isUpgradeActive() {
        return UpgradeSupport.isActiveState(normalizeState(readRawState()));
    }

    public boolean isUpgradeRequired() {
        return isUpgradeRequired(readRawState(), listPendingMigrations());
    }

    private boolean isUpgradeRequired(String rawState, List<String> pendingMigrations) {
        if (isUpgradeSkipped()) {
            return false;
        }
        if (installStateService.needsRestart()) {
            return false;
        }
        if (!InstallConfigSupport.isInstallCompletedOnDisk(installStateService.getConfigPath())) {
            return false;
        }
        String state = normalizeState(rawState);
        if (UpgradeSupport.STATE_RESTART_REQUIRED.equals(state) || UpgradeSupport.STATE_RUNNING.equals(state)) {
            return true;
        }
        return pendingMigrations != null && !pendingMigrations.isEmpty();
    }

    public Map<String, Object> getStatus() {
        syncPendingUpgradeStateWhenSchemaDrift();
        syncCompletedUpgradeStateWhenNoWorkNeeded();
        Map<String, Object> section = readSection();
        Map<String, Object> status = new HashMap<>(12);
        String state = normalizeState(stringValue(section.get("state")));
        List<String> pendingMigrations = listPendingMigrations();
        status.put("upgradeRequired", isUpgradeRequired(state, pendingMigrations));
        status.put("state", state);
        status.put("targetVersion", StringUtils.isNotEmpty(stringValue(section.get("targetVersion")))
                ? stringValue(section.get("targetVersion")) : version);
        status.put("completedVersion", stringValue(section.get("completedVersion")));
        status.put("lastError", stringValue(section.get("lastError")));
        status.put("lastStep", stringValue(section.get("lastStep")));
        status.put("lastBackupFile", stringValue(section.get("lastBackupFile")));
        status.put("attemptCount", numberValue(section.get("attemptCount")));
        status.put("skipped", isUpgradeSkipped());
        status.put("restartRequired", UpgradeSupport.STATE_RESTART_REQUIRED.equals(readRawState()));
        status.put("pendingMigrations", pendingMigrations);
        status.put("installed", installStateService.isInstalled());
        status.put("currentVersion", version);
        return status;
    }

    public void markPending() {
        if (isUpgradeSkipped()) {
            return;
        }
        Map<String, Object> section = readSection();
        if (UpgradeSupport.isActiveState(stringValue(section.get("state")))) {
            return;
        }
        section.put("state", UpgradeSupport.STATE_PENDING);
        section.put("targetVersion", version);
        section.put("attemptCount", 0);
        section.put("lastError", "");
        section.put("lastStep", "");
        section.put("startedAt", Instant.now().toString());
        writeSection(section);
        log.info("Legacy 实例已标记为待升级");
    }

    public void markRunning() {
        Map<String, Object> section = readSection();
        section.put("state", UpgradeSupport.STATE_RUNNING);
        section.put("attemptCount", numberValue(section.get("attemptCount")) + 1);
        writeSection(section);
    }

    public void markFailed(String lastStep, String error) {
        Map<String, Object> section = readSection();
        section.put("state", UpgradeSupport.STATE_FAILED);
        section.put("lastStep", lastStep);
        section.put("lastError", error != null ? error : "");
        writeSection(section);
    }

    public void markRestartRequired() {
        Map<String, Object> section = readSection();
        section.put("state", UpgradeSupport.STATE_RESTART_REQUIRED);
        section.put("lastStep", "");
        section.put("lastError", "");
        writeSection(section);
    }

    public void updateLastStep(String lastStep) {
        Map<String, Object> section = readSection();
        section.put("lastStep", lastStep);
        writeSection(section);
    }

    public void recordLastBackupFile(String backupFile) {
        Map<String, Object> section = readSection();
        section.put("lastBackupFile", backupFile != null ? backupFile : "");
        writeSection(section);
    }

    public String getLastBackupFile() {
        return stringValue(readSection().get("lastBackupFile"));
    }

    public void resetAfterRollback(String message) {
        Map<String, Object> section = readSection();
        section.put("state", UpgradeSupport.STATE_PENDING);
        section.put("lastStep", "");
        section.put("lastError", message != null ? message : "");
        writeSection(section);
    }

    public String getLastStep() {
        return stringValue(readSection().get("lastStep"));
    }

    public String resolveState() {
        syncFromDisk();
        return normalizeState(readRawState());
    }

    private String readRawState() {
        return stringValue(readSection().get("state"));
    }

    private String normalizeState(String rawState) {
        if (!UpgradeSupport.STATE_COMPLETED.equals(rawState)) {
            return rawState;
        }
        if (isUpgradeSkipped() || InstallConfigSupport.isInstallCompletedOnDisk(installStateService.getConfigPath())) {
            return rawState;
        }
        return rawState;
    }

    public boolean hasPendingSchemaUpgrade() {
        return !listPendingMigrations().isEmpty();
    }

    private void resetStaleUpgradeStateWhenInstallIncomplete() {
        if (isUpgradeSkipped()
                || InstallConfigSupport.isInstallCompletedOnDisk(installStateService.getConfigPath())
                || !isUpgradeActive()) {
            return;
        }
        if (UpgradeSupport.STATE_RESTART_REQUIRED.equals(readRawState())) {
            return;
        }
        Map<String, Object> section = readSection();
        section.put("state", UpgradeSupport.STATE_COMPLETED);
        section.put("lastError", "");
        section.put("lastStep", "");
        section.put("attemptCount", 0);
        section.put("completedVersion", "");
        writeSection(section);
        log.info("安装未完成，已清除无效的升级状态，请通过 /setup 完成安装");
    }

    private void syncCompletedUpgradeStateWhenNoWorkNeeded() {
        if (isUpgradeSkipped() || !InstallConfigSupport.isInstallCompletedOnDisk(installStateService.getConfigPath())) {
            return;
        }
        if (installStateService.needsRestart()) {
            return;
        }
        if (hasPendingSchemaUpgrade()) {
            return;
        }
        String state = readRawState();
        if (UpgradeSupport.STATE_RESTART_REQUIRED.equals(state) || UpgradeSupport.STATE_RUNNING.equals(state)) {
            return;
        }
        if (UpgradeSupport.STATE_PENDING.equals(state) || UpgradeSupport.STATE_FAILED.equals(state)) {
            markUpgradeCompleted("无待执行数据库迁移，升级状态已标记为 completed");
        }
    }

    private void markUpgradeCompleted(String logMessage) {
        Map<String, Object> section = readSection();
        section.put("state", UpgradeSupport.STATE_COMPLETED);
        section.put("completedVersion", version);
        section.put("lastError", "");
        section.put("lastStep", "");
        writeSection(section);
        if (StringUtils.isNotEmpty(logMessage)) {
            log.info(logMessage);
        }
    }

    private void syncPendingUpgradeStateWhenSchemaDrift() {
        if (isUpgradeSkipped() || !InstallConfigSupport.isInstallCompletedOnDisk(installStateService.getConfigPath())) {
            return;
        }
        if (installStateService.needsRestart()) {
            return;
        }
        if (!hasPendingSchemaUpgrade()) {
            return;
        }
        String state = readRawState();
        if (UpgradeSupport.STATE_COMPLETED.equals(state) || StringUtils.isEmpty(state)) {
            markPending();
            log.info("检测到待执行 Flyway 迁移，升级状态已同步为 pending");
        }
    }

    private void markInstallCompletedOnDiskIfNeeded() {
        if (InstallConfigSupport.isInstallCompletedOnDisk(installStateService.getConfigPath())) {
            return;
        }
        try {
            Path target = InstallConfigSupport.resolveConfigPath(installStateService.getConfigPath());
            Map<String, Object> root = InstallConfigSupport.loadYamlMap(target);
            if (root == null) {
                return;
            }
            InstallConfigSupport.setInstallCompleted(root, true);
            InstallConfigSupport.writeInstallConfig(target, root);
            log.info("升级完成，已写入 cat2bug.install.completed=true");
        } catch (Exception ex) {
            log.warnf("升级完成后无法写入 install.completed: %s", ex.getMessage());
        }
    }

    public List<String> listPendingMigrations() {
        try {
            return setupMigrationService.listPendingMigrations(resolveDatabaseType());
        } catch (Exception e) {
            return List.of();
        }
    }

    private String resolveDatabaseType() {
        return "mysql".equalsIgnoreCase(databaseId) ? "mysql" : "h2";
    }

    private Map<String, Object> readSection() {
        String statePath = upgradeStatePath.filter(StringUtils::isNotEmpty).orElse(null);
        Map<String, Object> fromDisk = InstallConfigSupport.readUpgradeState(
                installStateService.getConfigPath(),
                InstallConfigSupport.resolveUpgradeStatePath(
                        installStateService.getConfigPath(),
                        statePath).toString());
        if (!fromDisk.isEmpty()) {
            return new LinkedHashMap<>(fromDisk);
        }
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("state", cachedState);
        section.put("lastError", cachedLastError);
        section.put("lastStep", cachedLastStep);
        section.put("attemptCount", cachedAttemptCount);
        section.put("targetVersion", cachedTargetVersion);
        section.put("completedVersion", cachedCompletedVersion);
        return section;
    }

    private void writeSection(Map<String, Object> section) {
        try {
            String statePath = upgradeStatePath.filter(StringUtils::isNotEmpty).orElse(null);
            InstallConfigSupport.writeUpgradeState(
                    installStateService.getConfigPath(),
                    InstallConfigSupport.resolveUpgradeStatePath(
                            installStateService.getConfigPath(),
                            statePath).toString(),
                    section);
            cachedState = stringValue(section.get("state"));
            cachedLastError = stringValue(section.get("lastError"));
            cachedLastStep = stringValue(section.get("lastStep"));
            cachedAttemptCount = numberValue(section.get("attemptCount"));
            cachedTargetVersion = stringValue(section.get("targetVersion"));
            cachedCompletedVersion = stringValue(section.get("completedVersion"));
        } catch (Exception ex) {
            throw new IllegalStateException("无法写入升级状态: " + ex.getMessage(), ex);
        }
    }

    private void syncFromDisk() {
        String statePath = upgradeStatePath.filter(StringUtils::isNotEmpty).orElse(null);
        Map<String, Object> section = InstallConfigSupport.readUpgradeState(
                installStateService.getConfigPath(),
                InstallConfigSupport.resolveUpgradeStatePath(
                        installStateService.getConfigPath(),
                        statePath).toString());
        if (section.isEmpty()) {
            return;
        }
        cachedState = stringValue(section.get("state"));
        cachedLastError = stringValue(section.get("lastError"));
        cachedLastStep = stringValue(section.get("lastStep"));
        cachedAttemptCount = numberValue(section.get("attemptCount"));
        cachedTargetVersion = stringValue(section.get("targetVersion"));
        cachedCompletedVersion = stringValue(section.get("completedVersion"));
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int numberValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }
}
