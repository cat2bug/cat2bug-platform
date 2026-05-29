package com.cat2bug.system.domain.excel;

import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.poi.ExcelHandlerAdapter;
import com.cat2bug.system.domain.SysCaseStep;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.List;
import java.util.Map;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-10-15 16:15
 * @Version: 1.0.0
 */
public class CaseStepAdapter implements ExcelHandlerAdapter {
    @Override
    public Object format(Object value, String[] args, Cell cell, Workbook wb, Map<String, Object> requestParams) {
        cell.getCellStyle().setWrapText(true);
        cell.getRow().setHeight((short) -1);
        if (!(value instanceof List<?> rawList)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rawList.size(); i++) {
            Object item = rawList.get(i);
            if (!(item instanceof SysCaseStep s)) {
                continue;
            }
            if (StringUtils.isNotBlank(s.getStepExpect()) || StringUtils.isNotBlank(s.getStepDescribe())) {
                String enterKey = (i == rawList.size() - 1 ? "" : "\n");
                sb.append(String.format("%s%s%s%s",
                        StringUtils.isBlank(s.getStepDescribe()) ? "" : s.getStepDescribe(),
                        StringUtils.isBlank(s.getStepExpect()) ? "" : "---",
                        StringUtils.isBlank(s.getStepExpect()) ? "" : s.getStepExpect(),
                        enterKey));
            }
        }
        return sb.toString();
    }
}
