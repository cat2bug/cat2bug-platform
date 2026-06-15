package com.cat2bug.system.util;

import com.alibaba.fastjson2.JSONObject;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.domain.SysProjectDefectField;
import com.cat2bug.system.mapper.SysProjectDefectFieldMapper;
import com.cat2bug.system.support.MessageSourceTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

/**
 * 缺陷 custom_fields 值校验：剥离未定义键、必填与类型。
 */
@RunWith(MockitoJUnitRunner.class)
public class DefectCustomFieldsValidatorTest {

    @Mock
    private SysProjectDefectFieldMapper fieldMapper;

    @InjectMocks
    private DefectCustomFieldsValidator validator;

    private Runnable messageSourceCleanup;

    @Before
    public void setUpMessageSource() {
        messageSourceCleanup = MessageSourceTestSupport.installMessageSource();
    }

    @After
    public void tearDownMessageSource() {
        if (messageSourceCleanup != null) {
            messageSourceCleanup.run();
        }
    }

    private static SysProjectDefectField def(String key, String type, int required) {
        SysProjectDefectField f = new SysProjectDefectField();
        f.setFieldKey(key);
        f.setFieldLabel(key);
        f.setFieldType(type);
        f.setRequired(required);
        return f;
    }

    @Test
    public void validateAndStrip_removesUnknownKeys() {
        when(fieldMapper.selectEnabledListByProjectId(1L)).thenReturn(Collections.singletonList(
                def("severity", "string", 0)));
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("severity", "high");
        input.put("unknown", "x");
        Map<String, Object> out = validator.validateAndStrip(1L, input);
        assertEquals(1, out.size());
        assertEquals("high", out.get("severity"));
    }

    @Test
    public void validateAndStrip_requiredMissing_throws() {
        when(fieldMapper.selectEnabledListByProjectId(1L)).thenReturn(Collections.singletonList(
                def("severity", "string", 1)));
        try {
            validator.validateAndStrip(1L, Collections.emptyMap());
            fail("expected ServiceException");
        } catch (ServiceException e) {
            assertFalse(e.getMessage() == null || e.getMessage().isEmpty());
        }
    }

    @Test
    public void validateAndStrip_enumInvalidKey_throws() {
        SysProjectDefectField f = def("status", "enum", 0);
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("options", Collections.singletonList(
                new JSONObject().fluentPut("key", "open").fluentPut("label", "Open")));
        f.setTypeConfig(cfg);
        when(fieldMapper.selectEnabledListByProjectId(1L)).thenReturn(Collections.singletonList(f));
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("status", "closed");
        try {
            validator.validateAndStrip(1L, input);
            fail("expected ServiceException");
        } catch (ServiceException e) {
            assertFalse(e.getMessage() == null || e.getMessage().isEmpty());
        }
    }

    @Test
    public void validateAndStrip_booleanAndNumber_ok() {
        when(fieldMapper.selectEnabledListByProjectId(1L)).thenReturn(Arrays.asList(
                def("flag", "boolean", 0),
                def("count", "number", 0)));
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("flag", true);
        input.put("count", 3);
        Map<String, Object> out = validator.validateAndStrip(1L, input);
        assertEquals(true, out.get("flag"));
        assertEquals(3, out.get("count"));
    }

    @Test
    public void validateAndStrip_imageCommaString_normalizedToList() {
        when(fieldMapper.selectEnabledListByProjectId(1L)).thenReturn(Collections.singletonList(
                def("photo", "image", 0)));
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("photo", "/profile/upload/2026/06/a.png,/profile/upload/2026/06/b.png");
        Map<String, Object> out = validator.validateAndStrip(1L, input);
        @SuppressWarnings("unchecked")
        List<String> urls = (List<String>) out.get("photo");
        assertEquals(2, urls.size());
        assertEquals("/profile/upload/2026/06/a.png", urls.get(0));
        assertEquals("/profile/upload/2026/06/b.png", urls.get(1));
    }

    @Test
    public void normalizeDefaultValue_enumKey_ok() {
        SysProjectDefectField f = def("status", "enum", 0);
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("options", Collections.singletonList(
                new JSONObject().fluentPut("key", "open").fluentPut("label", "Open")));
        f.setTypeConfig(cfg);
        Object out = validator.normalizeDefaultValue(f, "open");
        assertEquals("open", out);
    }

    @Test
    public void validateAndStrip_stringTooLong_throws() {
        SysProjectDefectField f = def("note", "string", 0);
        f.setMaxLength(2);
        when(fieldMapper.selectEnabledListByProjectId(1L)).thenReturn(Collections.singletonList(f));
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("note", "abc");
        try {
            validator.validateAndStrip(1L, input);
            fail("expected ServiceException");
        } catch (ServiceException e) {
            assertFalse(e.getMessage() == null || e.getMessage().isEmpty());
        }
    }
}
