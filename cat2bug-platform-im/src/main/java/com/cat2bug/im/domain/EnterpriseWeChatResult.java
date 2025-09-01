package com.cat2bug.im.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-26 17:19
 * @Version: 1.0.0
 */
@Data
public class EnterpriseWeChatResult {
    /**
     * 出错返回码，为0表示成功，非0表示调用失败
     */
    private int errcode;
    /**
     * 返回码提示语
     */
    private String errmsg;
}
