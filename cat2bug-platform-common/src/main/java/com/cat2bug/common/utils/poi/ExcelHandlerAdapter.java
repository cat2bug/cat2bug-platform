package com.cat2bug.common.utils.poi;

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
     * @param requestParams 请求参数
     *
     * @return 处理后的值
     */
    Object format(Object value, String[] args, Map<String, Object> requestParams);
}
