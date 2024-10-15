package com.cat2bug.system.domain.excel;

import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.common.utils.poi.ExcelHandlerAdapter;
import com.cat2bug.system.domain.SysCaseStep;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
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
//        cell.getRow().getSheet().autoSizeColumn(0);
        StringBuffer sb = new StringBuffer();
        List<SysCaseStep> list = (List<SysCaseStep>) value;
        for(int i=0;i<list.size();i++) {
            SysCaseStep s = list.get(i);
            if(s!=null && StringUtils.isNotBlank(s.getStepExpect()) || StringUtils.isNotBlank(s.getStepDescribe())) {
                String enterKey = (i==list.size()-1?"":"\n");
                sb.append(String.format("%s - %s%s", s.getStepDescribe(), s.getStepExpect(), enterKey));
            }
        }
        return sb.toString();
    }
}
