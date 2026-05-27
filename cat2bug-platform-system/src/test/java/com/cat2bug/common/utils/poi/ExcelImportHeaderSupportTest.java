package com.cat2bug.common.utils.poi;

import com.cat2bug.common.annotation.Excel;
import org.junit.Test;
import org.springframework.context.MessageSource;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * Excel 导入表头多语言候选。
 */
public class ExcelImportHeaderSupportTest {

    @Test
    public void collectCandidates_includesAlternateKeyEnglishTitle() throws Exception {
        Field caseNameField = com.cat2bug.system.domain.SysCase.class.getDeclaredField("caseName");
        Excel excel = caseNameField.getAnnotation(Excel.class);

        MessageSource messageSource = new MessageSource() {
            @Override
            public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
                if ("case.name_excel".equals(code) && Locale.US.equals(locale)) {
                    return "Case Name(Required)";
                }
                if ("case.name".equals(code) && Locale.SIMPLIFIED_CHINESE.equals(locale)) {
                    return "用例名称";
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

        Set<String> candidates = ExcelImportHeaderSupport.collectImportHeaderCandidates(
                excel, messageSource, "用例名称(必填)");

        assertTrue(candidates.contains("Case Name(Required)"));
    }
}
