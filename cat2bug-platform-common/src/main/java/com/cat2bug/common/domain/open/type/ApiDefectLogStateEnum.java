package com.cat2bug.common.domain.open.type;

/**
 * Open API 缺陷日志状态，对齐 Spring {@code com.cat2bug.api.domain.type.ApiDefectLogStateEnum}。
 */
public enum ApiDefectLogStateEnum {
    CREATE,
    ASSIGN,
    REJECT,
    PASS,
    REJECTED,
    REPAIR,
    CLOSED,
    OPEN,
    UPDATE,
    IMPORT
}
