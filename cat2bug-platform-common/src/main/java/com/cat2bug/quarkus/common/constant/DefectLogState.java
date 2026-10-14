package com.cat2bug.quarkus.common.constant;

/**
 * 缺陷日志类型（与 Spring {@code SysDefectLogStateEnum} ordinal 对齐）。
 */
public enum DefectLogState {
    CREATE,
    ASSIGN,
    REJECT,
    PASS,
    REJECTED,
    REPAIR,
    CLOSED,
    OPEN,
    UPDATE,
    IMPORT,
    DELETE,
    RESTORE
}
