package com.cat2bug.common.config;

import java.util.Set;

/**
 * 升级状态常量与活跃状态判定。
 */
public final class UpgradeSupport {

    public static final String STATE_PENDING = InstallConfigSupport.UPGRADE_STATE_PENDING;
    public static final String STATE_RUNNING = InstallConfigSupport.UPGRADE_STATE_RUNNING;
    public static final String STATE_FAILED = InstallConfigSupport.UPGRADE_STATE_FAILED;
    public static final String STATE_RESTART_REQUIRED = InstallConfigSupport.UPGRADE_STATE_RESTART_REQUIRED;
    public static final String STATE_COMPLETED = InstallConfigSupport.UPGRADE_STATE_COMPLETED;

    public static final String STEP_BACKUP = InstallConfigSupport.UPGRADE_LAST_STEP_BACKUP;
    public static final String STEP_CONFIG = InstallConfigSupport.UPGRADE_LAST_STEP_CONFIG;
    public static final String STEP_MIGRATION = InstallConfigSupport.UPGRADE_LAST_STEP_MIGRATION;

    private static final Set<String> ACTIVE_STATES = Set.of(
            STATE_PENDING, STATE_RUNNING, STATE_FAILED, STATE_RESTART_REQUIRED);

    private UpgradeSupport() {
    }

    public static boolean isActiveState(String state) {
        return state != null && ACTIVE_STATES.contains(state);
    }
}
