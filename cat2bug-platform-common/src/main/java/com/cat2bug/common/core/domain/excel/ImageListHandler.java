package com.cat2bug.common.core.domain.excel;

import com.cat2bug.common.utils.poi.ExcelHandlerAdapter;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 图片列表字段格式化（JVM 导出嵌入由 {@code ExcelUtil} 按 {@code ColumnType.IMAGE_LIST} 处理）。
 */
public class ImageListHandler implements ExcelHandlerAdapter {
    @Override
    public Object format(Object value, String[] args, Map<String, Object> requestParams) {
        if (value == null || "".equals(value)) {
            return "";
        }
        return Stream.of(String.valueOf(value).split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
    }
}
