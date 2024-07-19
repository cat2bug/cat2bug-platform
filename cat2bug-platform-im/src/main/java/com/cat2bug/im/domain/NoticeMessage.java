package com.cat2bug.im.domain;

import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-08 14:03
 * @Version: 1.0.0
 */
@Data
public class NoticeMessage extends IMMessage<String> {
    /**
     * 项目ID
     */
    private Long projectId;
    /**
     * 公告状态（0正常 1关闭）
     */
    private char status;
    /**
     * 公告类型（1通知 2公告）
     */
    private char noticeType;
    /**
     * 是否开启背景音乐
     */
    private boolean backgroundMusic;
    /**
     * 是否开启提示面板
     */
    private boolean panel;
}
