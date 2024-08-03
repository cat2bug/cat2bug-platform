package com.cat2bug.common.core.domain.excel;

import com.cat2bug.common.core.domain.type.SysDefectTypeEnum;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.poi.ExcelHandlerAdapter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Map;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-09 17:43
 * @Version: 1.0.0
 */
public class DefectLevelHandler implements ExcelHandlerAdapter {
    @Override
    public Object format(Object value, String[] args, Cell cell, Workbook wb, Map<String, Object> requestParams) {
        String v = (String) value;
        return MessageUtils.message(v);
    }
}
