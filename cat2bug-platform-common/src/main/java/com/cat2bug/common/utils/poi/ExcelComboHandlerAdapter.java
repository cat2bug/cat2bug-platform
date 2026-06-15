package com.cat2bug.common.utils.poi;

import java.util.List;
import java.util.Map;

/**
 * Excel 下拉框数据处理器
 */
public interface ExcelComboHandlerAdapter {
    /**
     * 返回下拉选项列表
     *
     * @param args excel 上下文参数
     * @return 下拉选项
     */
    List<String> format(Map<String, Object> args);
}
