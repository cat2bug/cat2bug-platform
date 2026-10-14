package com.cat2bug.common.domain.open.type;

/**
 * Open API 缺陷状态，对齐 Spring {@code com.cat2bug.api.domain.type.ApiDefectStateEnum}。
 */
public enum ApiDefectStateEnum {
    PROCESSING,
    AUDIT,
    RESOLVED,
    REJECTED,
    CLOSED
}
