package com.cat2bug.common.core.domain.excel;

import com.cat2bug.common.core.domain.type.SysDefectTypeEnum;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.poi.ExcelHandlerAdapter;

import java.util.Map;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-09 17:43
 * @Version: 1.0.0
 */
public class DefectLevelHandler implements ExcelHandlerAdapter {
    @Override
    public Object format(Object value, String[] args, Map<String, Object> requestParams) {
        if (value == null) {
            return null;
        }
        String v = String.valueOf(value);
        String dictValue = DefectLevelImportLabelResolver.resolve(v);
        String canonical = dictValue != null ? dictValue : v;
        if (requestParams != null && Boolean.TRUE.equals(requestParams.get("_excelImportMode"))) {
            return canonical;
        }
        return MessageUtils.message(canonical);
    }
}
