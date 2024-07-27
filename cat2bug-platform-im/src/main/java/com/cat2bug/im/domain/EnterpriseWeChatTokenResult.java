package com.cat2bug.im.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-27 13:21
 * @Version: 1.0.0
 * 获取企业微信token返回信息
 */
@Data
public class EnterpriseWeChatTokenResult extends EnterpriseWeChatResult {
    /**
     * 获取到的凭证，最长为512字节
     */
    private String access_token;
    /**
     * 凭证的有效时间（秒）
     */
    private int expires_in;
}
