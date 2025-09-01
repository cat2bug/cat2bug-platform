package com.cat2bug.common.core.domain.excel;

import com.cat2bug.common.utils.DictUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.poi.ExcelComboHandlerAdapter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-09 12:14
 * @Version: 1.0.0
 */
public class DefectStateComboHandlerAdapter  implements ExcelComboHandlerAdapter {
    @Override
    public List<String> format(Map<String, Object> args, Cell cell, Workbook wb) {
        return Arrays.asList(
                MessageUtils.message("PROCESSING"),
                MessageUtils.message("AUDIT"),
                MessageUtils.message("REJECTED"),
                MessageUtils.message("CLOSED"));
    }
}
