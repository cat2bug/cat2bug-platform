package com.cat2bug.api.domain;

import lombok.Data;

import java.util.List;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-11-03 23:16
 * @Version: 1.0.0
 */
@Data
public class ApiDefectHandle {
    /**
     * 缺陷编号
     */
    private Long defectNum;

    /**
     * 描述
     */
    private String describe;
    /**
     * 处理人账号
     */
    private List<String> handlerAccountList;
}
