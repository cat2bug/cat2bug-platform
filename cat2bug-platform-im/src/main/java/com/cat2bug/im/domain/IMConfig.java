package com.cat2bug.im.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-18 00:23
 * @Version: 1.0.0
 * 消息配置类
 */
@Data
public class IMConfig implements Serializable {
    /**
     * 发送数据的模块配置
     */
    private Map<String, Object> modules;
    /**
     * 发送平台配置
     */
    private IMPlatformConfig platforms;
}
