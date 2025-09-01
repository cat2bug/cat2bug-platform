package com.cat2bug.common.core.domain.excel;

import com.cat2bug.common.utils.DictUtils;
import com.cat2bug.common.utils.MessageUtils;
import com.cat2bug.common.utils.bean.BeanUtils;
import com.cat2bug.common.utils.poi.ExcelComboHandlerAdapter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-09 02:44
 * @Version: 1.0.0
 */
public class ModuleComboHandlerAdapter implements ExcelComboHandlerAdapter {
    @Override
    public List<String> format(Map<String, Object> args, Cell cell, Workbook wb) {
        if(args.containsKey("moduleNameList")) {
            return (List)args.get("moduleNameList");
        } else {
            return new ArrayList<>();
        }
    }
}
