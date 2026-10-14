package com.cat2bug.quarkus.common.excel;

/**
 * 表头样式。
 */
public enum ExcelHeaderStyle {
    /** 深灰底 + 白色粗体（系统管理导出） */
    DARK_BAR,
    /** 浅灰底 + 粗体 */
    NORMAL,
    /** 浅灰底 + 红色粗体（必填列） */
    REQUIRED
}
