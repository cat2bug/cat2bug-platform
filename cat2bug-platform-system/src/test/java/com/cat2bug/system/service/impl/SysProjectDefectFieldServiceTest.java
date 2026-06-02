package com.cat2bug.system.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.cat2bug.common.exception.ServiceException;
import com.cat2bug.system.domain.SysProjectDefectField;
import com.cat2bug.system.domain.SysProjectDefectFieldColumnLayout;
import com.cat2bug.system.mapper.SysProjectDefectFieldMapper;
import com.cat2bug.system.util.DefectBuiltinFieldRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 缺陷自定义字段定义：枚举校验与启用数量上限。
 */
@RunWith(MockitoJUnitRunner.class)
public class SysProjectDefectFieldServiceTest {

    @Mock
    private SysProjectDefectFieldMapper fieldMapper;

    @InjectMocks
    private SysProjectDefectFieldServiceImpl fieldService;

    @Test
    public void validateTypeConfig_enumEmptyOptions_throws() {
        try {
            SysProjectDefectFieldServiceImpl.validateTypeConfig("enum", Collections.emptyMap());
            fail("expected ServiceException");
        } catch (ServiceException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void validateTypeConfig_enumDuplicateKey_throws() {
        JSONObject cfg = new JSONObject();
        cfg.put("options", new Object[]{
                new JSONObject().fluentPut("key", "a").fluentPut("label", "A"),
                new JSONObject().fluentPut("key", "a").fluentPut("label", "A2")
        });
        try {
            SysProjectDefectFieldServiceImpl.validateTypeConfig("enum", cfg);
            fail("expected ServiceException");
        } catch (ServiceException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void validateTypeConfig_enumValid_passes() {
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("options", new Object[]{
                new JSONObject().fluentPut("key", "p0").fluentPut("label", "P0").fluentPut("color", "#f00"),
                new JSONObject().fluentPut("key", "p1").fluentPut("label", "P1")
        });
        SysProjectDefectFieldServiceImpl.validateTypeConfig("enum", cfg);
    }

    @Test
    public void insert_enabledLimitExceeded_throws() {
        when(fieldMapper.selectByProjectIdAndFieldKey(any(), any())).thenReturn(null);
        when(fieldMapper.countEnabledByProjectId(1L)).thenReturn(50);

        SysProjectDefectField field = new SysProjectDefectField();
        field.setProjectId(1L);
        field.setFieldKey("severity");
        field.setFieldLabel("严重程度");
        field.setFieldType("enum");
        field.setEnabled(1);
        field.setTypeConfig(optionConfig("p0", "P0"));

        try {
            fieldService.insert(field);
            fail("expected ServiceException");
        } catch (ServiceException e) {
            assertNotNull(e.getMessage());
        }
    }

    @Test
    public void insert_validEnum_returnsField() {
        when(fieldMapper.selectByProjectIdAndFieldKey(any(), any())).thenReturn(null);
        when(fieldMapper.countEnabledByProjectId(1L)).thenReturn(0);
        when(fieldMapper.insert(any())).thenAnswer(inv -> {
            SysProjectDefectField f = inv.getArgument(0);
            f.setFieldId(99L);
            return 1;
        });

        SysProjectDefectField field = new SysProjectDefectField();
        field.setProjectId(1L);
        field.setFieldKey("severity");
        field.setFieldLabel("严重程度");
        field.setFieldType("enum");
        field.setEnabled(1);
        field.setTypeConfig(optionConfig("p0", "P0"));

        SysProjectDefectField created = fieldService.insert(field);
        assertEquals(Long.valueOf(99L), created.getFieldId());
    }

    @Test
    public void selectColumnLayoutByProjectId_respectsBuiltinEnabledFlag() {
        when(fieldMapper.selectDefectBuiltinFieldConfig(eq(1L))).thenReturn(
                "{\"handleBy\":{\"enabled\":0,\"sortOrder\":30},\"defectType\":{\"enabled\":1,\"sortOrder\":10}}");
        when(fieldMapper.selectEnabledListByProjectId(1L)).thenReturn(Collections.emptyList());

        SysProjectDefectFieldColumnLayout layout = fieldService.selectColumnLayoutByProjectId(1L);
        assertNotNull(layout.getEnabledBuiltinFieldKeys());
        assertEquals(true, layout.getEnabledBuiltinFieldKeys().contains("defectType"));
        assertEquals(true, layout.getEnabledBuiltinFieldKeys().contains("handleBy"));
        assertEquals(true, layout.getEnabledBuiltinFieldKeys().contains("defectName"));
        assertNotNull(layout.getOrderedEnabledFieldKeys());
        assertEquals(true, layout.getOrderedEnabledFieldKeys().contains("defectType"));
        assertEquals(true, layout.getOrderedEnabledFieldKeys().indexOf("defectType")
                < layout.getOrderedEnabledFieldKeys().indexOf("handleBy"));
    }

    private static Map<String, Object> optionConfig(String key, String label) {
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("options", new Object[]{
                new JSONObject().fluentPut("key", key).fluentPut("label", label)
        });
        return cfg;
    }
}
