package com.cat2bug.system.service.impl;

import com.cat2bug.system.domain.SysMemberOfDefectsLine;
import com.cat2bug.system.service.ISysDashboardService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SysDefectStatisticServiceTrendLineTest {

    @Mock
    private ISysDashboardService sysDashboardService;

    @InjectMocks
    private SysDefectStatisticServiceImpl service;

    @Test
    public void memberDefectLine_delegatesToDashboard() {
        SysMemberOfDefectsLine row = new SysMemberOfDefectsLine();
        row.setUserId(7L);
        row.setNickName("u1");
        row.setCreateTime("2026-05-10");
        row.setDefectTodayCount(1);
        when(sysDashboardService.memberOfDefectsLine(eq(1L), eq("day")))
                .thenReturn(Collections.singletonList(row));

        Map<String, Object> result = service.memberDefectLine(1L, "day");
        assertNotNull(result.get("time"));
        @SuppressWarnings("unchecked")
        List<SysMemberOfDefectsLine> data = (List<SysMemberOfDefectsLine>) result.get("data");
        assertEquals(Long.valueOf(7L), data.get(0).getUserId());
    }
}
