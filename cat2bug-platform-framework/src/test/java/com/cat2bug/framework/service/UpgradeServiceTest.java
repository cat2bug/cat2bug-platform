package com.cat2bug.framework.service;

import com.cat2bug.common.config.Cat2BugConfig;
import com.cat2bug.common.config.InstallConfigSupport;
import com.cat2bug.common.config.InstallProperties;
import com.cat2bug.common.config.UpgradeProperties;
import com.cat2bug.common.config.UpgradeSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpgradeServiceTest
{
    @Mock
    private InstallService installService;

    @InjectMocks
    private UpgradeService upgradeService;

    private UpgradeProperties upgradeProperties;
    private InstallProperties installProperties;
    private Cat2BugConfig cat2BugConfig;

    @BeforeEach
    void setUp()
    {
        upgradeProperties = new UpgradeProperties();
        installProperties = new InstallProperties();
        cat2BugConfig = new Cat2BugConfig();
        cat2BugConfig.setVersion("0.6.2");
        ReflectionTestUtils.setField(upgradeService, "upgradeProperties", upgradeProperties);
        ReflectionTestUtils.setField(upgradeService, "installProperties", installProperties);
        ReflectionTestUtils.setField(upgradeService, "cat2BugConfig", cat2BugConfig);
        ReflectionTestUtils.setField(upgradeService, "environment", new StandardEnvironment());
    }

    @Test
    void isUpgradeSkipped_returnsTrueFromProperty()
    {
        upgradeProperties.setSkip(true);

        assertTrue(upgradeService.isUpgradeSkipped());
    }

    @Test
    void isUpgradeRequired_returnsFalseWhenSkipped()
    {
        upgradeProperties.setSkip(true);

        assertFalse(upgradeService.isUpgradeRequired());
    }

    @Test
    void isUpgradeRequired_returnsTrueWhenLegacyWithoutInstallFile()
    {
        when(installService.hasLegacyInstallation()).thenReturn(true);

        assertTrue(upgradeService.isUpgradeRequired());
    }

    @Test
    void isUpgradeRequired_returnsFalseWhenEmptyDatabaseEvenIfSchemaPendingWouldExist()
    {
        when(installService.hasLegacyInstallation()).thenReturn(false);
        when(installService.isSchemaPresent()).thenReturn(false);

        assertFalse(upgradeService.isUpgradeRequired());
    }

    @Test
    void isUpgradeRequired_returnsTrueWhenFlywayHistoryPresentWithoutAdmin()
    {
        when(installService.hasLegacyInstallation()).thenReturn(false);
        when(installService.isSchemaPresent()).thenReturn(true);

        assertTrue(upgradeService.isUpgradeRequired());
    }

    @Test
    void isUpgradeActive_returnsTrueForPendingState(@TempDir Path dir) throws Exception
    {
        Path stateFile = dir.resolve("application-upgrade-state.yml");
        installProperties.setConfigPath(dir.resolve("missing-install.yml").toString());
        upgradeProperties.setStatePath(stateFile.toString());
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("state", UpgradeSupport.STATE_PENDING);
        InstallConfigSupport.writeUpgradeState(
                installProperties.getConfigPath(),
                upgradeProperties.getStatePath(),
                section);

        assertTrue(upgradeService.isUpgradeActive());
        assertEquals(UpgradeSupport.STATE_PENDING, upgradeService.resolveState());
    }

    @Test
    void isUpgradeActive_returnsFalseForCompletedStateWhenFullyInstalled()
    {
        when(installService.hasLegacyInstallation()).thenReturn(false);
        when(installService.isSchemaPresent()).thenReturn(false);
        upgradeProperties.setState(UpgradeSupport.STATE_COMPLETED);

        assertFalse(upgradeService.isUpgradeActive());
        assertEquals(UpgradeSupport.STATE_COMPLETED, upgradeService.resolveState());
    }

    @Test
    void resolveState_normalizesDefaultCompletedToPendingWhenLegacyStillNeedsUpgrade()
    {
        when(installService.hasLegacyInstallation()).thenReturn(true);
        upgradeProperties.setState(UpgradeSupport.STATE_COMPLETED);

        assertTrue(upgradeService.isUpgradeActive());
        assertEquals(UpgradeSupport.STATE_PENDING, upgradeService.resolveState());
    }

    @Test
    void markPending_writesPendingStateToSidecarFile(@TempDir Path dir) throws Exception
    {
        Path stateFile = dir.resolve("application-upgrade-state.yml");
        installProperties.setConfigPath(dir.resolve("missing-install.yml").toString());
        upgradeProperties.setStatePath(stateFile.toString());

        upgradeService.markPending();

        assertTrue(Files.isRegularFile(stateFile));
        @SuppressWarnings("unchecked")
        Map<String, Object> root = new Yaml().load(Files.readString(stateFile));
        assertEquals(UpgradeSupport.STATE_PENDING, nested(root, "cat2bug", "upgrade", "state"));
        assertEquals("0.6.2", nested(root, "cat2bug", "upgrade", "targetVersion"));
    }

    @Test
    void markPending_isIdempotentWhenAlreadyActive(@TempDir Path dir) throws Exception
    {
        Path stateFile = dir.resolve("application-upgrade-state.yml");
        installProperties.setConfigPath(dir.resolve("missing-install.yml").toString());
        upgradeProperties.setStatePath(stateFile.toString());
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("state", UpgradeSupport.STATE_FAILED);
        section.put("lastError", "flyway");
        InstallConfigSupport.writeUpgradeState(
                installProperties.getConfigPath(),
                upgradeProperties.getStatePath(),
                section);

        upgradeService.markPending();

        assertEquals(UpgradeSupport.STATE_FAILED, upgradeService.resolveState());
    }

    @SuppressWarnings("unchecked")
    private static Object nested(Map<String, Object> root, String... path)
    {
        Object current = root;
        for (String key : path)
        {
            if (!(current instanceof Map<?, ?> map))
            {
                return null;
            }
            current = ((Map<String, Object>) map).get(key);
        }
        return current;
    }
}
