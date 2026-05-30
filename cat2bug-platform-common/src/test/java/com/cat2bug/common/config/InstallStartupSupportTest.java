package com.cat2bug.common.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallStartupSupportTest
{
    @Test
    void bootstrapProperties_usesDedicatedH2FileNotDefaultDatabaseName()
    {
        Map<String, Object> props = InstallStartupSupport.bootstrapProperties();
        assertEquals("true", props.get(InstallStartupSupport.BOOTSTRAP_MODE_PROPERTY));
        String url = String.valueOf(props.get("spring.datasource.druid.master.url"));
        assertEquals(InstallStartupSupport.BOOTSTRAP_H2_JDBC_URL, url);
        assertTrue(url.contains(".cat2bug_bootstrap"));
        assertTrue(!url.contains("cat2bug_platform"));
    }

    @Test
    @EnabledIf("jarDirectoryAvailable")
    void pinResolvedInstallConfigPathIfPresent_usesJarDirectoryFallback(@TempDir Path tempDir) throws Exception
    {
        Path jarDir = InstallConfigSupport.resolveJarDirectory();
        String relative = "config/install/pin-test-" + UUID.randomUUID() + ".yml";
        Path jarConfig = jarDir.resolve(relative);
        Files.createDirectories(jarConfig.getParent());
        Files.writeString(jarConfig, "cat2bug:\n  install:\n    completed: true\n");

        String originalConfigPath = System.getProperty("cat2bug.install.config-path");
        String originalImport = System.getProperty("spring.config.import");
        try
        {
            System.setProperty("cat2bug.install.config-path", relative);
            System.clearProperty("spring.config.import");

            InstallStartupSupport.pinResolvedInstallConfigPathIfPresent();

            assertEquals(jarConfig.toAbsolutePath().normalize().toString(),
                    System.getProperty("cat2bug.install.config-path"));
            assertEquals("optional:file:" + jarConfig.toAbsolutePath().normalize(),
                    System.getProperty("spring.config.import"));
            assertFalse(InstallStartupSupport.evaluate(relative, "false").bootstrap());
        }
        finally
        {
            Files.deleteIfExists(jarConfig);
            restoreProperty("cat2bug.install.config-path", originalConfigPath);
            restoreProperty("spring.config.import", originalImport);
        }
    }

    static boolean jarDirectoryAvailable()
    {
        return InstallConfigSupport.resolveJarDirectory() != null;
    }

    private static void restoreProperty(String key, String value)
    {
        if (value == null)
        {
            System.clearProperty(key);
        }
        else
        {
            System.setProperty(key, value);
        }
    }
}
