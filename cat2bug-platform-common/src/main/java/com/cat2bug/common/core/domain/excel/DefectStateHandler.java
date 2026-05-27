package com.cat2bug.common.core.domain.excel;

import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.poi.ExcelHandlerAdapter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-09 12:07
 * @Version: 1.0.0
 */
public class DefectStateHandler implements ExcelHandlerAdapter {
    @Override
    public Object format(Object value, String[] args, Cell cell, Workbook wb, Map<String, Object> requestParams) {
        if(value == null) {
            return null;
        } else if(value instanceof SysDefectStateEnum) {
            SysDefectStateEnum v = (SysDefectStateEnum) value;
            return MessageUtils.message(v.name());
        } else {
            return DefectImportLabelResolver.resolveState((String) value);
        }
    }
}
