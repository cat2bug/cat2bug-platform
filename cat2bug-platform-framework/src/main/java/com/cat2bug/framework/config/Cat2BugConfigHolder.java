package com.cat2bug.framework.config;

import com.cat2bug.common.config.Cat2BugConfig;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * 启动时把 cat2bug.profile / cat2bug.git.* 写入 {@link Cat2BugConfig} 静态访问器。
 */
@ApplicationScoped
public class Cat2BugConfigHolder {

    @ConfigProperty(name = "cat2bug.profile", defaultValue = "./upload")
    String profile;

    @ConfigProperty(name = "cat2bug.git.root", defaultValue = "git")
    String gitRoot;

    @ConfigProperty(name = "cat2bug.git.import-timeout-seconds", defaultValue = "120")
    int importTimeoutSeconds;

    void onStart(@Observes StartupEvent event) {
        Cat2BugConfig.configure(profile, gitRoot, importTimeoutSeconds);
    }
}
