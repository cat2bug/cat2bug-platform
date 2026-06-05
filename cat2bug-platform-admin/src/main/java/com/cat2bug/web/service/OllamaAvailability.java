package com.cat2bug.web.service;

import com.cat2bug.ai.service.IAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 判断系统是否已配置并启用 Ollama 服务（{@code cat2bug.ai.enabled=true} 且 Bean 已加载）。
 */
@Component
public class OllamaAvailability
{
    private static final String SERVICE_TYPE_OLLAMA = "ollama";

    @Autowired(required = false)
    private Map<String, IAiService> aiServiceMap;

    public boolean isAvailable()
    {
        return aiServiceMap != null && aiServiceMap.get(SERVICE_TYPE_OLLAMA) != null;
    }
}
