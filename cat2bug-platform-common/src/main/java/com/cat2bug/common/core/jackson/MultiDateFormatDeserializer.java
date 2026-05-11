package com.cat2bug.common.core.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 支持多种日期格式的反序列化器
 *
 * @author yuzhantao
 * @date 2026-05-11
 */
public class MultiDateFormatDeserializer extends JsonDeserializer<Date> {

    private static final String[] DATE_FORMATS = new String[] {
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd",
        "yyyy/MM/dd HH:mm:ss",
        "yyyy/MM/dd"
    };

    @Override
    public Date deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        String dateString = jsonParser.getText();
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        for (String format : DATE_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false);
                return sdf.parse(dateString);
            } catch (ParseException e) {
                // 尝试下一个格式
            }
        }

        throw new IOException("无法解析日期: " + dateString +
            "。支持的格式: yyyy-MM-dd HH:mm:ss, ISO 8601 等");
    }
}
