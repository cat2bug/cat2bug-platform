package com.cat2bug.common.core.domain.excel;

import com.cat2bug.common.utils.poi.ExcelHandlerAdapter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-06-08 18:41
 * @Version: 1.0.0
 */
public class UrlListHandler implements ExcelHandlerAdapter {
    @Override
    public Object format(Object value, String[] args, Cell cell, Workbook wb, Map<String, Object> requestParams) {
        String v = (String)value;
        if(v==null || "".equals(v)) {
            return "";
        }

        String[] urls = v.split(",");
        return Arrays.stream(urls).map(url->{
            if(requestParams==null || !requestParams.containsKey("host"))
                return url;
            else {
                return requestParams.get("host")+url;
            }
        }).collect(Collectors.joining("\n"));
    }
}
