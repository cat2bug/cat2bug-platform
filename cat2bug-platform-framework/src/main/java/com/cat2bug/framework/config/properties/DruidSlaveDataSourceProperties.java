package com.cat2bug.framework.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 从库连接属性。
 */
@ConfigurationProperties(prefix = "spring.datasource.druid.slave")
public class DruidSlaveDataSourceProperties
{
    private boolean enabled;
    private String url;
    private String username;
    private String password;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

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
