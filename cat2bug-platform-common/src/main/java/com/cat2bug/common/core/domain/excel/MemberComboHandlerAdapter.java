package com.cat2bug.common.core.domain.excel;

import com.cat2bug.common.utils.poi.ExcelComboHandlerAdapter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-09 02:44
 * @Version: 1.0.0
 */
public class MemberComboHandlerAdapter implements ExcelComboHandlerAdapter {
    @Override
    public List<String> format(Map<String, Object> args, Cell cell, Workbook wb) {
        if(args.containsKey("memberList")) {
            return (List)args.get("memberList");
        } else {
            return new ArrayList<>();
        }
    }
}
