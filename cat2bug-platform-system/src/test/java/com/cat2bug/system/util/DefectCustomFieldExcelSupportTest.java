package com.cat2bug.system.util;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.utils.poi.ExcelColumnExportSupport;
import com.cat2bug.common.utils.poi.ExcelUtilJvmBridge;
import com.cat2bug.common.utils.poi.ExcelUtil;
import com.cat2bug.system.domain.SysProjectDefectField;
import com.cat2bug.system.support.MessageSourceTestSupport;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缺陷导出自定义列：writeSheet 后插入列，且不得再次 writeSheet 覆盖。
 */
public class DefectCustomFieldExcelSupportTest {

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

    @Test
    public void applyCustomColumnsAfterWrite_insertsCustomHeaderAndValue() {
        SysDefect defect = new SysDefect();
        Map<String, Object> cf = new HashMap<>();
        cf.put("testField", "hello-custom");
        defect.setCustomFields(cf);

        SysProjectDefectField def = new SysProjectDefectField();
        def.setFieldKey("testField");
        def.setFieldLabel("测试自定义");
        def.setFieldType("string");
        def.setRequired(0);

        String exportColumns = "["
                + "{\"key\":\"defect.name\",\"prop\":\"defectName\",\"visible\":true},"
                + "{\"key\":\"custom:testField\",\"prop\":\"custom_testField\",\"visible\":true}"
                + "]";

        Map<String, Object> params = new HashMap<>();
        params.put(ExcelColumnExportSupport.PARAM_EXPORT_COLUMNS, exportColumns);
        params.put(ExcelColumnExportSupport.PARAM_EXPORT_SCOPE, ExcelColumnExportSupport.SCOPE_DATA);

        ExcelUtil<SysDefect> util = new ExcelUtil<>(SysDefect.class);
        ExcelUtilJvmBridge.apply(util, params, ExcelColumnExportSupport.DEFECT_DATA_MAP, null, null);
        util.init(Collections.singletonList(defect), "缺陷数据", "", com.cat2bug.common.annotation.Excel.Type.EXPORT, params, null);
        util.writeSheet();
        DefectCustomFieldExcelSupport.applyCustomColumnsAfterWrite(util, params,
                ExcelColumnExportSupport.DEFECT_DATA_MAP, null, null,
                Collections.singletonList(defect), Collections.singletonList(def));

        Sheet sheet = util.getSheet();
        Assert.assertNotNull(sheet);
        Row header = sheet.getRow(util.getHeaderRowIndex());
        Assert.assertNotNull(header);
        boolean foundHeader = false;
        boolean foundValue = false;
        for (int c = 0; c < header.getLastCellNum(); c++) {
            if (header.getCell(c) != null && "测试自定义".equals(header.getCell(c).getStringCellValue())) {
                foundHeader = true;
            }
        }
        Row data = sheet.getRow(util.getDataStartRowIndex());
        Assert.assertNotNull(data);
        for (int c = 0; c < data.getLastCellNum(); c++) {
            if (data.getCell(c) != null && "hello-custom".equals(data.getCell(c).getStringCellValue())) {
                foundValue = true;
            }
        }
        Assert.assertTrue("应写入自定义列表头", foundHeader);
        Assert.assertTrue("应写入自定义列数据", foundValue);
    }

    @Test
    public void applyCustomColumnsAfterWrite_enumColumnHasDropdownValidation() {
        SysDefect defect = new SysDefect();
        Map<String, Object> cf = new HashMap<>();
        cf.put("priority", "high");
        defect.setCustomFields(cf);

        SysProjectDefectField def = new SysProjectDefectField();
        def.setFieldKey("priority");
        def.setFieldLabel("优先级");
        def.setFieldType("enum");
        Map<String, Object> typeConfig = new HashMap<>();
        typeConfig.put("options", java.util.Arrays.asList(
                java.util.Collections.singletonMap("key", "low"),
                new HashMap<String, String>() {{
                    put("key", "high");
                    put("label", "高");
                }}
        ));
        def.setTypeConfig(typeConfig);

        String exportColumns = "["
                + "{\"key\":\"defect.name\",\"prop\":\"defectName\",\"visible\":true},"
                + "{\"key\":\"custom:priority\",\"prop\":\"custom_priority\",\"visible\":true}"
                + "]";

        Map<String, Object> params = new HashMap<>();
        params.put(ExcelColumnExportSupport.PARAM_EXPORT_COLUMNS, exportColumns);
        params.put(ExcelColumnExportSupport.PARAM_EXPORT_SCOPE, ExcelColumnExportSupport.SCOPE_DATA);

        ExcelUtil<SysDefect> util = new ExcelUtil<>(SysDefect.class);
        ExcelUtilJvmBridge.apply(util, params, ExcelColumnExportSupport.DEFECT_DATA_MAP, null, null);
        util.init(Collections.singletonList(defect), "缺陷数据", "", com.cat2bug.common.annotation.Excel.Type.EXPORT, params, null);
        util.writeSheet();
        DefectCustomFieldExcelSupport.applyCustomColumnsAfterWrite(util, params,
                ExcelColumnExportSupport.DEFECT_DATA_MAP, null, null,
                Collections.singletonList(defect), Collections.singletonList(def));

        Sheet sheet = util.getSheet();
        List<? extends DataValidation> validations = sheet.getDataValidations();
        Assert.assertNotNull(validations);
        Assert.assertFalse("枚举自定义列应设置数据有效性下拉", validations.isEmpty());
    }
}
