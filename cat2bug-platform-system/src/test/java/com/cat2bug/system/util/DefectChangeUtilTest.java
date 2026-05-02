package com.cat2bug.system.util;

import com.cat2bug.common.core.domain.entity.SysDefect;
import com.cat2bug.common.core.domain.type.SysDefectTypeEnum;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 校验缺陷字段差异工具的核心规则。
 */
public class DefectChangeUtilTest {

    @Test
    public void noChange_returnsEmpty() {
        SysDefect a = baseDefect();
        SysDefect b = baseDefect();

        List<DefectChange> changes = DefectChangeUtil.diff(a, b);

        assertTrue("未做任何修改时不应产生变更日志", changes.isEmpty());
    }

    @Test
    public void singleTextField_changes() {
        SysDefect oldDefect = baseDefect();
        SysDefect newDefect = baseDefect();
        newDefect.setDefectName("renamed");

        List<DefectChange> changes = DefectChangeUtil.diff(oldDefect, newDefect);

        assertEquals(1, changes.size());
        DefectChange c = changes.get(0);
        assertEquals("defectName", c.getField());
        assertEquals("text", c.getType());
        assertEquals("origin", c.getOldValue());
        assertEquals("renamed", c.getNewValue());
    }

    @Test
    public void longDescribeChange_truncatesSnapshotForPersistence() {
        SysDefect oldDefect = baseDefect();
        SysDefect newDefect = baseDefect();
        StringBuilder longDesc = new StringBuilder();
        for (int i = 0; i < 6000; i++) {
            longDesc.append('z');
        }
        newDefect.setDefectDescribe(longDesc.toString());

        List<DefectChange> changes = DefectChangeUtil.diff(oldDefect, newDefect);

        assertEquals(1, changes.size());
        DefectChange c = changes.get(0);
        assertEquals("defectDescribe", c.getField());
        assertTrue("日志快照应截断", String.valueOf(c.getNewValue()).endsWith("..."));
        assertTrue("日志快照应有上限",
                String.valueOf(c.getNewValue()).length() <= 4000 + "...".length());
    }

    @Test
    public void multipleFields_changeOrderFollowsWhitelist() {
        SysDefect oldDefect = baseDefect();
        SysDefect newDefect = baseDefect();
        newDefect.setDefectName("rename");
        newDefect.setDefectType(SysDefectTypeEnum.TASK);
        newDefect.setDefectDescribe("new description");

        List<DefectChange> changes = DefectChangeUtil.diff(oldDefect, newDefect);

        assertEquals(3, changes.size());
        assertEquals("defectName", changes.get(0).getField());
        assertEquals("defectType", changes.get(1).getField());
        assertEquals("defectDescribe", changes.get(2).getField());
    }

    @Test
    public void timeField_producesFormattedDisplay() {
        SysDefect oldDefect = baseDefect();
        SysDefect newDefect = baseDefect();
        newDefect.setPlanEndTime(date(2026, Calendar.MAY, 1, 10, 0, 0));

        List<DefectChange> changes = DefectChangeUtil.diff(oldDefect, newDefect);

        assertEquals(1, changes.size());
        DefectChange c = changes.get(0);
        assertEquals("planEndTime", c.getField());
        assertEquals("time", c.getType());
        assertNotNull(c.getNewDisplay());
        assertTrue(c.getNewDisplay().startsWith("2026-05-01"));
    }

    @Test
    public void imageList_changesRecordedAsImages() {
        SysDefect oldDefect = baseDefect();
        oldDefect.setImgUrls("a.png,b.png");
        SysDefect newDefect = baseDefect();
        newDefect.setImgUrls("a.png,c.png,d.png");

        List<DefectChange> changes = DefectChangeUtil.diff(oldDefect, newDefect);

        assertEquals(1, changes.size());
        DefectChange c = changes.get(0);
        assertEquals("imgUrls", c.getField());
        assertEquals("images", c.getType());
        assertEquals(Integer.valueOf(2), (Integer) c.getOldValue());
        assertEquals(Integer.valueOf(3), (Integer) c.getNewValue());
        assertEquals(Integer.valueOf(2), c.getUrlAddedCount());
        assertEquals(Integer.valueOf(1), c.getUrlRemovedCount());
    }

    @Test
    public void handleByOrder_treatedAsChange() {
        SysDefect oldDefect = baseDefect();
        oldDefect.setHandleBy(Arrays.asList(1L, 2L));
        SysDefect newDefect = baseDefect();
        newDefect.setHandleBy(Arrays.asList(2L, 1L));

        List<DefectChange> changes = DefectChangeUtil.diff(oldDefect, newDefect);

        assertEquals(1, changes.size());
        assertEquals("handleBy", changes.get(0).getField());
        assertEquals("users", changes.get(0).getType());
    }

    @Test
    public void nullVsEmpty_treatedAsSame() {
        SysDefect oldDefect = baseDefect();
        oldDefect.setDefectDescribe(null);
        SysDefect newDefect = baseDefect();
        newDefect.setDefectDescribe("");

        List<DefectChange> changes = DefectChangeUtil.diff(oldDefect, newDefect);

        assertTrue(changes.isEmpty());
    }

    @Test
    public void nullDefectInputs_returnsEmpty() {
        assertTrue(DefectChangeUtil.diff(null, baseDefect()).isEmpty());
        assertTrue(DefectChangeUtil.diff(baseDefect(), null).isEmpty());
    }

    private SysDefect baseDefect() {
        SysDefect d = new SysDefect();
        d.setDefectId(1L);
        d.setProjectId(10L);
        d.setDefectName("origin");
        d.setDefectType(SysDefectTypeEnum.BUG);
        d.setDefectLevel("middle");
        d.setHandleBy(Collections.singletonList(1L));
        d.setModuleId(100L);
        d.setModuleVersion("v1.0");
        d.setDefectDescribe("desc");
        d.setImgUrls(null);
        d.setAnnexUrls(null);
        return d;
    }

    private Date date(int y, int m, int d, int h, int min, int s) {
        Calendar c = Calendar.getInstance();
        c.set(y, m, d, h, min, s);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
