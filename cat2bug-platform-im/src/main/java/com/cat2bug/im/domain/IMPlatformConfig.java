package com.cat2bug.im.domain;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-18 00:29
 * @Version: 1.0.0
 */
@Data
public class IMPlatformConfig {
    @JsonProperty("asystem")
    private IMSystemPlatformConfig system;

    @JsonProperty("bmail")
    private IMMailPlatformConfig mail;
    private IMDingPlatformConfig ding;
    private EnterpriseWeChatPlatformConfig wechat;
}
