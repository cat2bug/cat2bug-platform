package com.cat2bug.im.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-28 01:37
 * @Version: 1.0.0
 * 钉钉获取token返回信息
 */
@Data
public class DingTokenResult {
    /**
     * 生成的accessToken
     */
    private String accessToken;
    /**
     * accessToken的过期时间，单位秒
     */
    private String expireIn;
}
