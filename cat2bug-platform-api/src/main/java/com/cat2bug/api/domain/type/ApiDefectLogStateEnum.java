package com.cat2bug.api.domain.type;

/**
 * 缺陷日志状态
 */
public enum ApiDefectLogStateEnum {
    /** 创建 */
    CREATE,
    /** 指派 */
    ASSIGN,
    /** 审核驳回 */
    REJECT,
    /** 审核通过 */
    PASS,
    /** 已驳回 */
    REJECTED,
    /** 已处理 */
    REPAIR,
    /** 已关闭 */
    CLOSED,
    /** 开启 */
    OPEN,
    /** 更新 */
    UPDATE
}
