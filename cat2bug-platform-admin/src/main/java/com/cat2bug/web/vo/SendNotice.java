package com.cat2bug.web.vo;

import lombok.Data;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-07-20 22:17
 * @Version: 1.0.0
 */
@Data
public class SendNotice {
    /**
     * 通知标题
     */
    private String title;
    /**
     * 通知内容
     */
    private String content;
    /**
     * 项目ID
     */
    private Long projectId;
    /**
     * 组名称
     */
    private String groupName;
    /**
     * 接收人ID集合
     */
    private List<Long> receiveIds;
    /**
     * 数据源地址
     */
    private String src;
}
