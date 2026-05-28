package com.cat2bug.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * 在 ConfigData 加载 profile 配置之前，将首次安装引导强制为 H2。
 */
public class InstallEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered
{
    private static final Logger log = LoggerFactory.getLogger(InstallEnvironmentPostProcessor.class);

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
        log.info("EnvironmentPostProcessor: 首次安装引导，强制 H2（配置文件 {} 尚未生成）", decision.configPath());
    }

    @Override
    public int getOrder()
    {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
