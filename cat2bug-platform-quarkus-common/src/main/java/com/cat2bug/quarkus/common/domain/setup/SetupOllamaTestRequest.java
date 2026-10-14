package com.cat2bug.quarkus.common.domain.setup;

/**
 * Ollama 连接测试请求
 */
public class SetupOllamaTestRequest
{
    private String host;

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }
}
