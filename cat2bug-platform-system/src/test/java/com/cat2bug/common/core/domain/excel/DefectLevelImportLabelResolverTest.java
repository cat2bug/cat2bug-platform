package com.cat2bug.common.core.domain.excel;

import com.cat2bug.common.core.domain.entity.SysDictData;
import org.junit.Test;
import org.springframework.context.MessageSource;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * 缺陷优先级 Excel 导入多语言标签解析。
 */
public class DefectLevelImportLabelResolverTest {

    @Test
    public void resolve_englishDisplayName_toDictValue() {
        MessageSource messageSource = new MessageSource() {
            @Override
            public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
                if ("urgent".equals(code) && Locale.US.equals(locale)) {
                    return "Urgent";
                }
                return code;
            }

            @Override
            public String getMessage(String code, Object[] args, Locale locale) {
                return getMessage(code, args, code, locale);
            }

            @Override
            public String getMessage(org.springframework.context.MessageSourceResolvable resolvable, Locale locale) {
                return resolvable.getDefaultMessage();
            }
        };
        SysDictData dict = new SysDictData();
        dict.setDictValue("urgent");
        dict.setDictLabel("急");
        Map<String, String> map = DefectLevelImportLabelResolver.buildLevelMap(
                messageSource, Collections.singletonList(dict));

        assertEquals("urgent", map.get("Urgent"));
        assertEquals("urgent", map.get("urgent"));
        assertEquals("urgent", map.get("急"));
    }

    @Test
    public void resolve_unknownLabel_returnsNull() {
        Map<String, String> map = DefectLevelImportLabelResolver.buildLevelMap(null, Collections.emptyList());
        assertNull(map.get("NotALevel"));
    }
}
