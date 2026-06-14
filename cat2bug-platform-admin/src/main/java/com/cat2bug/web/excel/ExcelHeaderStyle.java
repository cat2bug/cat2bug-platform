package com.cat2bug.web.excel;/**
 * 表头样式（B9 Excel 引擎 SPI）。
 */
public enum ExcelHeaderStyle {
    /** 深灰底 + 白字（系统管理导出） */
    DARK_BAR,
    /** 浅灰底 + 粗体 */
    NORMAL,
    /** 浅灰底 + 红色粗体（必填列） */
    REQUIRED
}
