package com.cat2bug.framework.service;

import com.cat2bug.common.config.InstallProperties;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstallConfigMigrationRunnerTest
{
    @Mock
    private InstallProperties installProperties;

    @Mock
    private InstallService installService;

    @InjectMocks
    private InstallConfigMigrationRunner migrationRunner;

    private StandardEnvironment environment;

    @BeforeEach
    void setUp()
    {
        environment = new StandardEnvironment();
        ReflectionTestUtils.setField(migrationRunner, "environment", environment);
    }

    @Test
    void run_skipsWhenInstallFileAlreadyPresent(@TempDir Path dir) throws Exception
    {
        Path installFile = dir.resolve("application-install.yml");
        Files.writeString(installFile, "cat2bug:\n  install:\n    completed: true\n");
        when(installProperties.isInstallConfigPresent()).thenReturn(true);

        migrationRunner.run(mock(ApplicationArguments.class));

        verify(installService, never()).hasLegacyInstallation();
    }

    @Test
    void run_skipsWhenNoLegacyIndicators() throws Exception
    {
        when(installProperties.isInstallConfigPresent()).thenReturn(false);
        when(installService.isInstallSkipped()).thenReturn(false);
        when(installService.hasLegacyInstallation()).thenReturn(false);
        when(installService.isSchemaPresent()).thenReturn(false);

        migrationRunner.run(mock(ApplicationArguments.class));

        verify(installProperties, never()).resolveConfigPath();
    }

    @Test
    void run_writesCompletedInstallWhenLegacyDetected(@TempDir Path dir) throws Exception
    {
        Path installFile = dir.resolve("application-install.yml");
        when(installProperties.isInstallConfigPresent()).thenReturn(false);
        when(installService.isInstallSkipped()).thenReturn(false);
        when(installService.hasLegacyInstallation()).thenReturn(true);
        when(installProperties.resolveConfigPath()).thenReturn(installFile);

        Map<String, Object> props = new HashMap<>();
        props.put("spring.database-type", "mysql");
        props.put("spring.datasource.druid.master.url", "jdbc:mysql://127.0.0.1:3306/cat2bug_platform");
        props.put("spring.datasource.druid.master.username", "root");
        props.put("spring.datasource.druid.master.password", "secret");
        props.put("cat2bug.cache.type", "local");
        props.put("cat2bug.profile", "uploadPath");
        props.put("cat2bug.temp", "uploadTemp");
        environment.getPropertySources().addFirst(new MapPropertySource("test", props));

        migrationRunner.run(mock(ApplicationArguments.class));

        assertTrue(Files.isRegularFile(installFile));
        @SuppressWarnings("unchecked")
        Map<String, Object> root = new Yaml().load(Files.readString(installFile));
        assertEquals(true, nested(root, "cat2bug", "install", "completed"));
        assertEquals("mysql", nested(root, "spring", "database-type"));
        assertTrue(String.valueOf(nested(root, "spring", "datasource", "druid", "master", "url")).contains("cat2bug_platform"));
        assertNotNull(nested(root, "spring", "datasource", "druid", "initialSize"));
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
