package com.cat2bug.framework.service;

import com.cat2bug.common.config.InstallProperties;
import com.cat2bug.common.config.InstallStartupSupport;
import com.cat2bug.system.domain.SysConfig;
import com.cat2bug.system.service.ISysConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * InstallService 安装状态检测单元测试。
 */
@ExtendWith(MockitoExtension.class)
class InstallServiceTest
{
    @Mock
    private InstallProperties installProperties;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ISysConfigService configService;

    @InjectMocks
    private InstallService installService;

    @BeforeEach
    void setUp()
    {
        ReflectionTestUtils.setField(installService, "installProperties", installProperties);
        ReflectionTestUtils.setField(installService, "jdbcTemplate", jdbcTemplate);
        ReflectionTestUtils.setField(installService, "configService", configService);
    }

    @Test
    void isInstalled_returnsTrueWhenSkipFlagSet()
    {
        when(installProperties.isSkip()).thenReturn(true);

        assertTrue(installService.isInstalled());
        verify(installProperties, never()).isInstallCompletedOnDisk();
    }

    @Test
    void isInstalled_returnsTrueWhenSkipFromEnv()
    {
        when(installProperties.isSkip()).thenReturn(false);
        when(installProperties.isSkipFromEnv()).thenReturn(true);

        assertTrue(installService.isInstalled());
        verify(installProperties, never()).isInstallCompletedOnDisk();
    }

    @Test
    void isInstalled_returnsTrueWhenCompletedTrueOnDisk(@TempDir Path dir) throws Exception
    {
        Path installFile = dir.resolve("application-install.yml");
        Files.writeString(installFile, """
                cat2bug:
                  install:
                    completed: true
                """);
        useRealInstallProperties(installFile);

        assertTrue(installService.isInstalled());
    }

    @Test
    void isInstalled_returnsFalseWhenFileExistsWithoutCompleted(@TempDir Path dir) throws Exception
    {
        Path installFile = dir.resolve("application-install.yml");
        Files.writeString(installFile, """
                cat2bug:
                  cache:
                    type: local
                """);
        useRealInstallProperties(installFile);

        assertFalse(installService.isInstalled());
    }

    @Test
    void isInstalled_returnsFalseWhenNoInstallFile(@TempDir Path dir)
    {
        Path missing = dir.resolve("missing-install.yml");
        useRealInstallProperties(missing);

        assertFalse(installService.isInstalled());
    }

    @Test
    void isInstalled_returnsFalseWhenOnlyInstallCompletedInDatabase()
    {
        when(installProperties.isSkip()).thenReturn(false);
        when(installProperties.isSkipFromEnv()).thenReturn(false);
        when(installProperties.isInstallCompletedOnDisk()).thenReturn(false);

        assertFalse(installService.isInstalled());
        verify(jdbcTemplate, never()).queryForObject(any(), eq(String.class), any());
    }

    @Test
    void needsRestart_returnsTrueWhenBootstrapModeAndCompletedOnDisk()
    {
        when(installProperties.isSkip()).thenReturn(false);
        when(installProperties.isInstallCompletedOnDisk()).thenReturn(true);
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test",
                Map.of(InstallStartupSupport.BOOTSTRAP_MODE_PROPERTY, "true")));
        ReflectionTestUtils.setField(installService, "environment", environment);

        assertTrue(installService.needsRestart());
    }

    @Test
    void needsRestart_returnsFalseAfterRestart()
    {
        when(installProperties.isSkip()).thenReturn(false);
        when(installProperties.isInstallCompletedOnDisk()).thenReturn(true);
        ReflectionTestUtils.setField(installService, "environment", new StandardEnvironment());

        assertFalse(installService.needsRestart());
    }

    @Test
    void needsRestart_returnsFalseWhenUpgradeAwaitingRestart()
    {
        when(installProperties.isSkip()).thenReturn(false);
        when(installProperties.isInstallCompletedOnDisk()).thenReturn(true);
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test",
                Map.of(InstallStartupSupport.BOOTSTRAP_MODE_PROPERTY, "true")));
        ReflectionTestUtils.setField(installService, "environment", environment);
        UpgradeService upgradeService = mock(UpgradeService.class);
        when(upgradeService.resolveState()).thenReturn("restart_required");
        ReflectionTestUtils.setField(installService, "upgradeService", upgradeService);

        assertFalse(installService.needsRestart());
    }

    @Test
    void hasLegacyAdminUser_returnsTrueWhenAdminPresent()
    {
        mockLegacyAdminCount(2);

        assertTrue(installService.hasLegacyAdminUser());
    }

    @Test
    void hasLegacyAdminUser_returnsFalseWhenNoAdmin()
    {
        mockLegacyAdminCount(0);

        assertFalse(installService.hasLegacyAdminUser());
    }

    @Test
    void run_doesNotAutoMarkCompletedOnLegacyAdmin()
    {
        installService.run(mock(ApplicationArguments.class));

        verify(configService, never()).insertConfig(any());
        verify(configService, never()).updateConfig(any());
    }

    private void useRealInstallProperties(Path installFile)
    {
        InstallProperties real = new InstallProperties();
        real.setConfigPath(installFile.toString());
        ReflectionTestUtils.setField(installService, "installProperties", real);
    }

    private void mockLegacyAdminCount(int count)
    {
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM sys_user WHERE user_name = ? AND del_flag = '0'"),
                eq(Integer.class),
                eq(InstallService.LEGACY_ADMIN_USERNAME)))
                .thenReturn(count);
    }
}
