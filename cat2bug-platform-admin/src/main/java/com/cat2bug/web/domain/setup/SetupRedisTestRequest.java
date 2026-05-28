package com.cat2bug.web.domain.setup;

/**
 * Redis 连接测试请求
 */
public class SetupRedisTestRequest
{
    private String host;

    private Integer port;

    private String password;

    private Integer database;

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public Integer getPort()
    {
        return port;
    }

    public void setPort(Integer port)
    {
        this.port = port;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public Integer getDatabase()
    {
        return database;
    }

    public void setDatabase(Integer database)
    {
        this.database = database;
    }
}
