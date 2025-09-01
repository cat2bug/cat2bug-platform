package com.cat2bug.common.utils.poi;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Map;

/**
 * Excel数据格式处理适配器
 * 
 * @author ruoyi
 */
public interface ExcelHandlerAdapter
{
    /**
     * 格式化
     * 
     * @param value 单元格数据值
     * @param args excel注解args参数组
     * @param cell 单元格对象
     * @param wb 工作簿对象
     * @param requestParams 请求参数
     *
     * @return 处理后的值
     */
    Object format(Object value, String[] args, Cell cell, Workbook wb, Map<String, Object> requestParams);
}
