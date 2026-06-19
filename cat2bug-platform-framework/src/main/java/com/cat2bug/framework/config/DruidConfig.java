package com.cat2bug.framework.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot3.autoconfigure.properties.DruidStatProperties;
import com.alibaba.druid.util.Utils;
import com.cat2bug.common.enums.DataSourceType;
import com.cat2bug.common.utils.spring.SpringUtils;
import com.cat2bug.framework.config.properties.DruidMasterDataSourceProperties;
import com.cat2bug.framework.config.properties.DruidProperties;
import com.cat2bug.framework.config.properties.DruidSlaveDataSourceProperties;
import com.cat2bug.framework.datasource.DynamicDataSource;
import com.google.common.base.Preconditions;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import jakarta.servlet.*;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * druid 配置多数据源
 * 
 * @author ruoyi
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ DruidMasterDataSourceProperties.class, DruidSlaveDataSourceProperties.class })
public class DruidConfig
{
    @Value("${spring.datasource.driverClassName:}")
    private String driverClassName;

    @Bean
    public DatabaseIdProvider getDatabaseIdProvider() {
        DatabaseIdProvider databaseIdProvider = new VendorDatabaseIdProvider() {
            @Override
            public String getDatabaseId(DataSource dataSource) {
                String databaseId = super.getDatabaseId(dataSource);
                Preconditions.checkNotNull(databaseId, "没有获取到databaseId");
                return databaseId;
            }
        };
        Properties p = new Properties();
        p.setProperty("H2", "h2");
        p.setProperty("MySQL", "mysql");
        p.setProperty("MariaDB", "mysql");
        databaseIdProvider.setProperties(p);
        return databaseIdProvider;
    }

    @Bean
    public DataSource masterDataSource(DruidProperties druidProperties,
            DruidMasterDataSourceProperties masterProperties)
    {
        DruidDataSource dataSource = new DruidDataSource();
        applyConnectionProperties(dataSource, masterProperties.getUrl(), masterProperties.getUsername(),
                masterProperties.getPassword());
        return druidProperties.dataSource(dataSource);
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.datasource.druid.slave", name = "enabled", havingValue = "true")
    public DataSource slaveDataSource(DruidProperties druidProperties,
            DruidSlaveDataSourceProperties slaveProperties)
    {
        DruidDataSource dataSource = new DruidDataSource();
        applyConnectionProperties(dataSource, slaveProperties.getUrl(), slaveProperties.getUsername(),
                slaveProperties.getPassword());
        return druidProperties.dataSource(dataSource);
    }

    private void applyConnectionProperties(DruidDataSource dataSource, String url, String username, String password)
    {
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        if (driverClassName != null && !driverClassName.isBlank())
        {
            dataSource.setDriverClassName(driverClassName);
        }
    }

    @Bean(name = "dynamicDataSource")
    @Primary
    public DynamicDataSource dataSource(DataSource masterDataSource)
    {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.MASTER.name(), masterDataSource);
        setDataSource(targetDataSources, DataSourceType.SLAVE.name(), "slaveDataSource");
        return new DynamicDataSource(masterDataSource, targetDataSources);
    }
    
    /**
     * 设置数据源
     * 
     * @param targetDataSources 备选数据源集合
     * @param sourceName 数据源名称
     * @param beanName bean名称
     */
    public void setDataSource(Map<Object, Object> targetDataSources, String sourceName, String beanName)
    {
        try
        {
            DataSource dataSource = SpringUtils.getBean(beanName);
            targetDataSources.put(sourceName, dataSource);
        }
        catch (Exception e)
        {
        }
    }

    /**
     * 去除监控页面底部的广告
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    @ConditionalOnProperty(name = "spring.datasource.druid.statViewServlet.enabled", havingValue = "true")
    public FilterRegistrationBean removeDruidFilterRegistrationBean(DruidStatProperties properties)
    {
        // 获取web监控页面的参数
        DruidStatProperties.StatViewServlet config = properties.getStatViewServlet();
        // 提取common.js的配置路径
        String pattern = config.getUrlPattern() != null ? config.getUrlPattern() : "/druid/*";
        String commonJsPattern = pattern.replaceAll("\\*", "js/common.js");
        final String filePath = "support/http/resources/js/common.js";
        // 创建filter进行过滤
        Filter filter = new Filter()
        {
            @Override
            public void init(jakarta.servlet.FilterConfig filterConfig) throws ServletException
            {
            }
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException
            {
                chain.doFilter(request, response);
                // 重置缓冲区，响应头不会被重置
                response.resetBuffer();
                // 获取common.js
                String text = Utils.readFromResource(filePath);
                // 正则替换banner, 除去底部的广告信息
                text = text.replaceAll("<a.*?banner\"></a><br/>", "");
                text = text.replaceAll("powered.*?shrek.wang</a>", "");
                response.getWriter().write(text);
            }
            @Override
            public void destroy()
            {
            }
        };
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns(commonJsPattern);
        return registrationBean;
    }
}
