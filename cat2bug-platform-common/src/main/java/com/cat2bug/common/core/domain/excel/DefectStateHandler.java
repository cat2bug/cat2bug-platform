package com.cat2bug.common.core.domain.excel;

import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
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
            switch (v) {
                case PROCESSING:
                    return "处理中";
                case AUDIT:
                    return "待审核";
                case RESOLVED:
                    return "已解决";
                case REJECTED:
                    return "已驳回";
                case CLOSED:
                    return "已关闭";
                default:
                    return "";
            }
        } else {
            String v = (String) value;
            switch (v) {
                case "处理中":
                    return SysDefectStateEnum.PROCESSING;
                case "待审核":
                    return SysDefectStateEnum.AUDIT;
                case "已解决":
                    return SysDefectStateEnum.RESOLVED;
                case "已驳回":
                    return SysDefectStateEnum.REJECTED;
                case "已关闭":
                    return SysDefectStateEnum.CLOSED;
                default:
                    return null;
            }
        }
    }
}
