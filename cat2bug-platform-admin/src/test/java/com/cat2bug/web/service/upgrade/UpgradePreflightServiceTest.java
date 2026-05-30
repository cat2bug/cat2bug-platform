package com.cat2bug.web.service.upgrade;

import com.cat2bug.common.config.InstallConfigSupport;
import com.cat2bug.common.config.InstallProperties;
import com.cat2bug.framework.service.UpgradeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpgradePreflightServiceTest
{
    @Mock
    private UpgradeService upgradeService;

    @Mock
    private UpgradeDatabaseBackupService upgradeDatabaseBackupService;

    @InjectMocks
    private UpgradePreflightService upgradePreflightService;

    private InstallProperties installProperties;

    @BeforeEach
    void setUp()
    {
        installProperties = new InstallProperties();
        ReflectionTestUtils.setField(upgradePreflightService, "installProperties", installProperties);
        ReflectionTestUtils.setField(upgradePreflightService, "environment", new StandardEnvironment());
        when(upgradeDatabaseBackupService.resolveDatabaseName(any())).thenReturn("cat2bug_platform");
        when(upgradeDatabaseBackupService.defaultBackupFileName(any())).thenReturn("cat2bug_platform_20260530_120000.sql");
        when(upgradeDatabaseBackupService.resolveBackupDirectory()).thenReturn(Paths.get("/tmp/cat2bug-backup-test"));
    }

    @Test
    void buildPreflight_readsInstallFromDiskAndOmitsRedisForLocalCache(@TempDir Path dir) throws Exception
    {
        Path installFile = dir.resolve("application-install.yml");
        installProperties.setConfigPath(installFile.toString());

        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> cat2bug = new LinkedHashMap<>();
        Map<String, Object> install = new LinkedHashMap<>();
        install.put("completed", true);
        Map<String, Object> cache = new LinkedHashMap<>();
        cache.put("type", "local");
        cat2bug.put("install", install);
        cat2bug.put("cache", cache);
        cat2bug.put("profile", "uploadPath");
        cat2bug.put("temp", "uploadTemp");
        root.put("cat2bug", cat2bug);

        Map<String, Object> spring = new LinkedHashMap<>();
        spring.put("database-type", "h2");
        Map<String, Object> master = new LinkedHashMap<>();
        master.put("url", "jdbc:h2:file:./data/cat2bug_platform;MODE=MySQL;DATABASE_TO_LOWER=TRUE;");
        Map<String, Object> druid = new LinkedHashMap<>();
        druid.put("master", master);
        Map<String, Object> datasource = new LinkedHashMap<>();
        datasource.put("druid", druid);
        spring.put("datasource", datasource);
        root.put("spring", spring);

        InstallConfigSupport.writeInstallConfig(installFile, root);

        when(upgradeService.getStatus()).thenReturn(Map.of(
                "currentVersion", "0.6.2",
                "targetVersion", "0.6.2",
                "pendingMigrations", List.of()));

        Map<String, Object> preflight = upgradePreflightService.buildPreflight();
        assertEquals("local", preflight.get("cacheType"));
        assertEquals("uploadPath", preflight.get("profile"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> diffs = (List<Map<String, Object>>) preflight.get("diffs");

        assertFalse(diffs.stream().anyMatch(row -> String.valueOf(row.get("key")).contains("redis")));
        assertFalse(diffs.stream().anyMatch(row ->
                "cat2bug.install.completed".equals(row.get("key"))));

        @SuppressWarnings("unchecked")
        Map<String, Object> suggested = (Map<String, Object>) preflight.get("suggestedConfig");
        Object suggestedCat2bug = suggested.get("cat2bug");
        assertTrue(suggestedCat2bug instanceof Map<?, ?>);
        @SuppressWarnings("unchecked")
        Object completed = ((Map<String, Object>) ((Map<String, Object>) suggestedCat2bug).get("install")).get("completed");
        assertEquals(true, completed);
    }

    @Test
    void buildPreflight_populatesMysqlFormFieldsFromInstall(@TempDir Path dir) throws Exception
    {
        Path installFile = dir.resolve("application-install.yml");
        installProperties.setConfigPath(installFile.toString());

        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> cat2bug = new LinkedHashMap<>();
        Map<String, Object> cache = new LinkedHashMap<>();
        cache.put("type", "local");
        cat2bug.put("cache", cache);
        cat2bug.put("profile", "./upload");
        root.put("cat2bug", cat2bug);

        Map<String, Object> master = new LinkedHashMap<>();
        master.put("url", "jdbc:mysql://127.0.0.1:3306/cat2bug_platform?useSSL=false");
        master.put("username", "root");
        master.put("password", "cat2bug_password");
        Map<String, Object> druid = new LinkedHashMap<>();
        druid.put("master", master);
        Map<String, Object> datasource = new LinkedHashMap<>();
        datasource.put("druid", druid);
        Map<String, Object> spring = new LinkedHashMap<>();
        spring.put("database-type", "mysql");
        spring.put("datasource", datasource);
        root.put("spring", spring);

        InstallConfigSupport.writeInstallConfig(installFile, root);
        when(upgradeService.getStatus()).thenReturn(Map.of(
                "currentVersion", "0.6.2",
                "targetVersion", "0.6.2",
                "pendingMigrations", List.of()));

        Map<String, Object> preflight = upgradePreflightService.buildPreflight();

        assertEquals("mysql", preflight.get("databaseType"));
        assertEquals("127.0.0.1", preflight.get("mysqlHost"));
        assertEquals(3306, preflight.get("mysqlPort"));
        assertEquals("cat2bug_platform", preflight.get("mysqlDatabase"));
        assertEquals("root", preflight.get("mysqlUsername"));
        assertEquals("cat2bug_password", preflight.get("mysqlPassword"));
    }

    @Test
    void buildPreflight_diffsCompareAgainstTemplate_notMerged(@TempDir Path dir) throws Exception
    {
        Path installFile = dir.resolve("application-install.yml");
        installProperties.setConfigPath(installFile.toString());

        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> cat2bug = new LinkedHashMap<>();
        Map<String, Object> cache = new LinkedHashMap<>();
        cache.put("type", "local");
        cat2bug.put("cache", cache);
        cat2bug.put("profile", "./custom-upload");
        root.put("cat2bug", cat2bug);

        Map<String, Object> spring = new LinkedHashMap<>();
        spring.put("database-type", "h2");
        root.put("spring", spring);

        InstallConfigSupport.writeInstallConfig(installFile, root);
        when(upgradeService.getStatus()).thenReturn(Map.of(
                "currentVersion", "0.6.2",
                "targetVersion", "0.6.2",
                "pendingMigrations", List.of()));

        Map<String, Object> preflight = upgradePreflightService.buildPreflight();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> diffs = (List<Map<String, Object>>) preflight.get("diffs");

        assertTrue(diffs.stream().anyMatch(row ->
                "cat2bug.profile".equals(row.get("key")) && "preserve".equals(row.get("action"))));
        assertFalse(diffs.stream().anyMatch(row -> "cat2bug.install.completed".equals(row.get("key"))));
    }
}
