package com.cat2bug.quarkus.system.service;

import com.cat2bug.quarkus.common.config.InstallConfigSupport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class InstallStateService {

    @ConfigProperty(name = "cat2bug.install.config-path", defaultValue = InstallConfigSupport.DEFAULT_CONFIG_PATH)
    String configPath;

    @ConfigProperty(name = "cat2bug.install.skip", defaultValue = "false")
    String skipProperty;

    @ConfigProperty(name = "cat2bug.bootstrap.mode", defaultValue = "true")
    boolean bootstrapMode;

    @Inject
    InstallBootstrapState installBootstrapState;

    public String getConfigPath() {
        return configPath;
    }

    public boolean isInstalled() {
        if (InstallConfigSupport.isInstallSkipped(skipProperty)) {
            return true;
        }
        return InstallConfigSupport.isInstallCompletedOnDisk(configPath);
    }

    public boolean isSkipped() {
        return InstallConfigSupport.isInstallSkipped(skipProperty);
    }

    public boolean needsRestart() {
        if (isSkipped()) {
            return false;
        }
        if (!InstallConfigSupport.isInstallCompletedOnDisk(configPath)) {
            return false;
        }
        if (installBootstrapState.isRuntimeActivated()) {
            return false;
        }
        return bootstrapMode;
    }
}
