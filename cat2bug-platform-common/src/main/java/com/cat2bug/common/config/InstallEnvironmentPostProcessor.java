package com.cat2bug.common.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * 在 ConfigData 加载可选 install 之前，将首次安装引导所需的 H2 classpath 模板注入 Environment。
 */
public class InstallEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered
{
    static final String BOOTSTRAP_PROPERTY_SOURCE = "cat2bugInstallBootstrap";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application)
    {
        String configPath = environment.getProperty("cat2bug.install.config-path", InstallConfigSupport.DEFAULT_CONFIG_PATH);
        InstallStartupSupport.BootstrapDecision decision = InstallStartupSupport.evaluate(
                configPath,
                environment.getProperty("cat2bug.install.skip", "false"));
        if (!decision.bootstrap())
        {
            return;
        }

        environment.getPropertySources().addFirst(
                new MapPropertySource(BOOTSTRAP_PROPERTY_SOURCE, InstallStartupSupport.bootstrapProperties()));
        InstallStartupSupport.logBootstrapNotice(decision.configPath());
    }

    @Override
    public int getOrder()
    {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
