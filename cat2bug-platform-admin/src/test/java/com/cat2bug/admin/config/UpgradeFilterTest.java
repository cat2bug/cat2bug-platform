package com.cat2bug.admin.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpgradeFilterTest {

    @Test
    void isAllowedDuringUpgrade_allowsUpgradeAndStaticAssets() {
        assertTrue(UpgradeFilter.isAllowedDuringUpgrade("/upgrade/status", "GET"));
        assertTrue(UpgradeFilter.isAllowedDuringUpgrade("/upgrade/preflight", "GET"));
        assertTrue(UpgradeFilter.isAllowedDuringUpgrade("/upgrade/submit", "POST"));
        assertTrue(UpgradeFilter.isAllowedDuringUpgrade("/version", "GET"));
        assertTrue(UpgradeFilter.isAllowedDuringUpgrade("/static/js/app.js", "GET"));
        assertTrue(UpgradeFilter.isAllowedDuringUpgrade("/setup/test/path", "POST"));
        assertTrue(UpgradeFilter.isAllowedDuringUpgrade("/setup/status", "GET"));
        assertFalse(UpgradeFilter.isAllowedDuringUpgrade("/system/user/list", "GET"));
        assertFalse(UpgradeFilter.isAllowedDuringUpgrade("/system/project/1", "GET"));
        assertFalse(UpgradeFilter.isAllowedDuringUpgrade("/login", "POST"));
    }

    @Test
    void isAllowedDuringUpgradeRestart_allowsAuthEndpoints() {
        assertTrue(UpgradeFilter.isAllowedDuringUpgradeRestart("/login", "POST"));
        assertTrue(UpgradeFilter.isAllowedDuringUpgradeRestart("/register", "POST"));
        assertTrue(UpgradeFilter.isAllowedDuringUpgradeRestart("/captchaImage", "GET"));
        assertTrue(UpgradeFilter.isAllowedDuringUpgradeRestart("/upgrade/status", "GET"));
        assertFalse(UpgradeFilter.isAllowedDuringUpgradeRestart("/system/project/1", "GET"));
    }
}
