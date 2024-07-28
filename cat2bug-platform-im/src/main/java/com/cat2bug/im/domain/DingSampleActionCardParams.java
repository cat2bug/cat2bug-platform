package com.cat2bug.im.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-28 09:58
 * @Version: 1.0.0
 * 钉钉卡片形式消息参数
 */
@Data
public class DingSampleActionCardParams {
    private String title;
    private String text;
    private String singleTitle;
    private String singleURL;
}
