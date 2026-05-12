package com.cat2bug.api.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-11-03 01:02
 * @Version: 1.0.0
 */
@Data
public class ApiDeliverable {
    /**
     * 交付物ID（Open API 响应中不输出，仅服务层使用）
     */
    @JsonIgnore
    private Long deliverableId;
    /**
     * 交付物父ID（Open API 响应中不输出，仅服务层使用）
     */
    @JsonIgnore
    private Long deliverablePid;
    /**
     * 交付物名称
     */
    private String deliverableName;
    /**
     * 交付物路径
     */
    private String deliverablePath;
    /**
     * 子项数量
     */
    private int childrenCount;
    /**
     * 备注
     */
    private String remark;
}
