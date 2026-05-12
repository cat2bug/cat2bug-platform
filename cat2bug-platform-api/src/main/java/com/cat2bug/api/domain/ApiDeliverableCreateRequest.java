package com.cat2bug.api.domain;

import lombok.Data;

/**
 * Open API 创建交付物请求体
 */
@Data
public class ApiDeliverableCreateRequest {
    /** 父级交付物全路径，多级用 {@code /} 分隔；不填表示挂在项目根下 */
    private String parentDeliverablePath;
    private String deliverableName;
    private String remark;
}
