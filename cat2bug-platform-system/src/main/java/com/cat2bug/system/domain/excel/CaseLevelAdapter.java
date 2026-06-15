package com.cat2bug.system.domain.excel;

import com.cat2bug.common.utils.poi.ExcelHandlerAdapter;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-10-21 22:33
 * @Version: 1.0.0
 */
public class CaseLevelAdapter implements ExcelHandlerAdapter {
    @Override
    public Object format(Object value, String[] args, Map<String, Object> requestParams) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(String.valueOf(value));

        // 找到匹配的字符串
        if (matcher.find()) {
            String numberStr = matcher.group();
            // 尝试将字符串转换为long
            try {
                return Long.parseLong(numberStr)+1;
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
