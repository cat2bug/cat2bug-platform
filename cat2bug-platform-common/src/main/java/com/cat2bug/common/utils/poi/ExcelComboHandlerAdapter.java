package com.cat2bug.common.utils.poi;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.List;
import java.util.Map;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-02-02 16:20
 * @Version: 1.0.0
 */
public interface ExcelComboHandlerAdapter {
    /**
     * 格式化
     *
     * @param args excel注解args参数组
     * @param cell 单元格对象
     * @param wb 工作簿对象
     *
     * @return 处理后的值
     */
    List<String> format(Map<String,Object> args, Cell cell, Workbook wb);
}
