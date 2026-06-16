package com.cat2bug.common.core.domain.excel;

import com.cat2bug.common.utils.poi.ExcelComboHandlerAdapter;

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
    @SuppressWarnings("unchecked")
    public List<String> format(Map<String, Object> args) {
        if(args.containsKey("memberList")) {
            return (List<String>)args.get("memberList");
        } else {
            return new ArrayList<>();
        }
    }
}
