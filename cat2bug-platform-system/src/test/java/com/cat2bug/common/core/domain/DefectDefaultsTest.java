package com.cat2bug.common.core.domain;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.common.core.domain.type.SysDefectTypeEnum;
import com.cat2bug.common.utils.poi.ExcelColumnExportSupport;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 缺陷类型、优先级、状态默认值。
 */
public class DefectDefaultsTest {

    @Test
    public void applyDefectDefaults_fillsAllWhenMissing() {
        SysDefect defect = new SysDefect();
        DefectDefaults.applyDefectDefaults(defect);
        assertEquals(SysDefectTypeEnum.BUG, defect.getDefectType());
        assertEquals("middle", defect.getDefectLevel());
        assertEquals(SysDefectStateEnum.PROCESSING, defect.getDefectState());
    }

    @Test
    public void applyDefectDefaults_blankLevelBecomesMiddle() {
        SysDefect defect = new SysDefect();
        defect.setDefectType(SysDefectTypeEnum.TASK);
        defect.setDefectLevel("   ");
        defect.setDefectState(SysDefectStateEnum.CLOSED);
        DefectDefaults.applyDefectDefaults(defect);
        assertEquals(SysDefectTypeEnum.TASK, defect.getDefectType());
        assertEquals("middle", defect.getDefectLevel());
        assertEquals(SysDefectStateEnum.CLOSED, defect.getDefectState());
    }

    @Test
    public void applyDefectDefaults_preservesExplicitValues() {
        SysDefect defect = new SysDefect();
        defect.setDefectType(SysDefectTypeEnum.DEMAND);
        defect.setDefectLevel("urgent");
        defect.setDefectState(SysDefectStateEnum.AUDIT);
        DefectDefaults.applyDefectDefaults(defect);
        assertEquals(SysDefectTypeEnum.DEMAND, defect.getDefectType());
        assertEquals("urgent", defect.getDefectLevel());
        assertEquals(SysDefectStateEnum.AUDIT, defect.getDefectState());
    }

    @Test
    public void importTemplateRequired_onlyNameAndAssignee() {
        assertTrue(ExcelColumnExportSupport.DEFECT_TEMPLATE_REQUIRED.contains("defectName"));
        assertTrue(ExcelColumnExportSupport.DEFECT_TEMPLATE_REQUIRED.contains("handleByNames"));
        assertEquals(2, ExcelColumnExportSupport.DEFECT_TEMPLATE_REQUIRED.size());
    }
}
