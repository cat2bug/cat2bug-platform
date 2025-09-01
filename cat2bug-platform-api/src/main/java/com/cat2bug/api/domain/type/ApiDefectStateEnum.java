package com.cat2bug.api.domain.type;

/**
 * @Author: yuzhantao
 * @CreateTime: 2023-11-28 14:01
 * @Version: 1.0.0
 * 缺陷状态
 */
public enum ApiDefectStateEnum {
    /** 待处理 */
    PROCESSING,
    /** 待验证 */
    AUDIT,
    /** 已解决 */
    RESOLVED,
    /** 已驳回 */
    REJECTED,
    /** 已关闭 */
    CLOSED
}
