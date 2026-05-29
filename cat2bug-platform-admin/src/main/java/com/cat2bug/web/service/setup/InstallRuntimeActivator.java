package com.cat2bug.web.service.setup;

import com.alibaba.druid.pool.DruidDataSource;
import com.cat2bug.common.config.InstallStartupSupport;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.framework.datasource.DynamicDataSource;
import com.cat2bug.web.domain.setup.SetupSubmitRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 安装向导提交后在不重启 JVM 的情况下激活 install 配置（切换数据源、退出引导模式）。
 */
@Service
public class InstallRuntimeActivator
{
    private static final Logger log = LoggerFactory.getLogger(InstallRuntimeActivator.class);

    public static final String RUNTIME_PROPERTY_SOURCE = "cat2bugInstallRuntime";

    @Autowired
    @Qualifier("dynamicDataSource")
    private DynamicDataSource dynamicDataSource;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private SetupSubmitDataSourceFactory setupSubmitDataSourceFactory;

    /**
     * @param mysqlDataSource MySQL 安装阶段已使用的连接池；为 null 时按请求新建
     * @return true 表示运行期已就绪，无需进程重启；false 表示仍需重启（如 Redis 缓存）
     */
    public boolean activateAfterSetup(SetupSubmitRequest request, DruidDataSource mysqlDataSource)
    {
        String databaseType = request.getDatabaseType().toLowerCase();
        String cacheType = StringUtils.isNotEmpty(request.getCacheType())
                ? request.getCacheType().toLowerCase() : "local";

        if ("redis".equals(cacheType))
        {
            log.info("安装向导：已选择 Redis 缓存，将自动重启进程以加载 J2Cache");
            return false;
        }

        if ("mysql".equals(databaseType))
        {
            DruidDataSource master = mysqlDataSource != null
                    ? mysqlDataSource
                    : setupSubmitDataSourceFactory.createMysqlDataSource(request);
            dynamicDataSource.replaceMaster(master);
            log.info("安装向导：运行期主库已热切换为 MySQL");
        }

        applyRuntimeProperties(request, databaseType, cacheType);
        return true;
    }

    private void applyRuntimeProperties(SetupSubmitRequest request, String databaseType, String cacheType)
    {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put(InstallStartupSupport.BOOTSTRAP_MODE_PROPERTY, "false");
        props.put("spring.database-type", databaseType);
        props.put("cat2bug.cache.type", cacheType);
        if (StringUtils.isNotEmpty(request.getProfile()))
        {
            props.put("cat2bug.profile", request.getProfile());
        }
        if (StringUtils.isNotEmpty(request.getTemp()))
        {
            props.put("cat2bug.temp", request.getTemp());
        }
        if (StringUtils.isNotEmpty(request.getLogPath()))
        {
            props.put("logging.file.path", request.getLogPath());
        }
        if (Boolean.TRUE.equals(request.getAiEnabled()) && StringUtils.isNotEmpty(request.getAiHost()))
        {
            props.put("cat2bug.ai.enabled", true);
            props.put("cat2bug.ai.host", request.getAiHost());
        }
        else
        {
            props.put("cat2bug.ai.enabled", false);
        }
        if (StringUtils.isNotEmpty(request.getAiDefaultModel()))
        {
            props.put("cat2bug.ai.default-business-model", request.getAiDefaultModel());
        }

        environment.getPropertySources().remove(RUNTIME_PROPERTY_SOURCE);
        environment.getPropertySources().addFirst(new MapPropertySource(RUNTIME_PROPERTY_SOURCE, props));
    }

    /** 供测试或诊断：当前主库是否为 H2 文件库 */
    boolean isMasterH2(DataSource dataSource)
    {
        if (!(dataSource instanceof DruidDataSource druid))
        {
            return false;
        }
        String url = druid.getUrl();
        return url != null && url.toLowerCase().contains(":h2:");
    }
}
