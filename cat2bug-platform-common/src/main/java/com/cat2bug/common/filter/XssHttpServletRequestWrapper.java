package com.cat2bug.common.filter;

import java.io.IOException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import com.cat2bug.common.utils.html.EscapeUtil;

/**
 * XSS过滤处理
 * 
 * @author ruoyi
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper
{
    /**
     * @param request
     */
    public XssHttpServletRequestWrapper(HttpServletRequest request)
    {
        super(request);
    }

    @Override
    public String[] getParameterValues(String name)
    {
        String[] values = super.getParameterValues(name);
        if (values != null)
        {
            int length = values.length;
            String[] escapesValues = new String[length];
            for (int i = 0; i < length; i++)
            {
                // 防xss攻击和过滤前后空格
                escapesValues[i] = EscapeUtil.clean(values[i]).trim();
            }
            return escapesValues;
        }
        return super.getParameterValues(name);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        /*
         * JSON 请求体禁止整段 EscapeUtil.clean / HTMLFilter。
         * 报告正文等含 <img src="..."> 时，过滤器会弄坏 JSON 字符串转义，
         * Jackson 报 Unexpected character ('a')...（见 /system/report PUT）。
         * 表单参数仍走 getParameterValues；字段级可用 @Xss。
         */
        return super.getInputStream();
    }
}
