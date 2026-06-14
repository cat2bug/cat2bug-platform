package com.cat2bug.framework.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 主库连接属性（独立 Properties 类，供 Native AOT 运行时绑定）。
 */
@ConfigurationProperties(prefix = "spring.datasource.druid.master")
public class DruidMasterDataSourceProperties
{
    private String url;
    private String username;
    private String password;

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }
}
