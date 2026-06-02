package com.cat2bug.system.util;

import com.cat2bug.common.core.domain.type.SysDefectStateEnum;
import com.cat2bug.system.domain.SysDefectLine;
import com.cat2bug.system.domain.SysMemberOfDefectsLine;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SysDefectTrendLineHelperTest {

    @Test
    public void buildDefectStateLineResult_groupsByStateAndTime() {
        SysDefectLine line = new SysDefectLine();
        line.setDefectState(SysDefectStateEnum.PROCESSING);
        line.setDefectTime("2026-05-01");
        line.setDefectCount(3L);

        Map<String, Object> result = SysDefectTrendLineHelper.buildDefectStateLineResult(
                Arrays.asList(line), "day");

        assertNotNull(result.get("times"));
        assertNotNull(result.get("data"));
        @SuppressWarnings("unchecked")
        Map<String, List<Long>> data = (Map<String, List<Long>>) result.get("data");
        assertTrue(data.containsKey("PROCESSING"));
    }

    @Test
    public void buildMemberDefectLineResult_preservesUserId() {
        SysMemberOfDefectsLine row = new SysMemberOfDefectsLine();
        row.setUserId(42L);
        row.setNickName("demo");
        row.setCreateTime("2026-05-01");
        row.setDefectTodayCount(2);

        Map<String, Object> result = SysDefectTrendLineHelper.buildMemberDefectLineResult(
                Arrays.asList(row), "day");

        @SuppressWarnings("unchecked")
        List<SysMemberOfDefectsLine> data = (List<SysMemberOfDefectsLine>) result.get("data");
        assertEquals(1, data.size());
        assertEquals(Long.valueOf(42L), data.get(0).getUserId());
    }
}
