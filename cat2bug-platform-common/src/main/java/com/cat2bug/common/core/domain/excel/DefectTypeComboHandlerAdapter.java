package com.cat2bug.common.core.domain.excel;

import com.cat2bug.common.core.domain.type.SysDefectTypeEnum;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.poi.ExcelComboHandlerAdapter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-09 17:46
 * @Version: 1.0.0
 */
public class DefectTypeComboHandlerAdapter implements ExcelComboHandlerAdapter {
    @Override
    public List<String> format(Map<String, Object> args) {
        return Arrays.stream(SysDefectTypeEnum.values()).map(t-> MessageUtils.message(t.toString())).collect(Collectors.toList());
    }
}
