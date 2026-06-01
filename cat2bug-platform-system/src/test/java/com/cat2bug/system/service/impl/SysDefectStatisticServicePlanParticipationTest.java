package com.cat2bug.system.service.impl;

import com.cat2bug.system.domain.SysColumnsInChart;
import com.cat2bug.system.domain.SysDefectParticipationDay;
import com.cat2bug.system.domain.SysPlan;
import com.cat2bug.system.domain.SysPlanMetricsItem;
import com.cat2bug.system.mapper.SysDefectStatisticMapper;
import com.cat2bug.system.service.ISysDashboardService;
import com.cat2bug.system.service.ISysPlanService;
import com.cat2bug.system.util.PlanBurndownUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 个人参与补齐、计划指标解析、燃尽补齐。
 */
public class SysDefectStatisticServicePlanParticipationTest {

    @Mock
    private SysDefectStatisticMapper sysDefectStatisticMapper;
    @Mock
    private ISysDashboardService sysDashboardService;
    @Mock
    private ISysPlanService sysPlanService;

    @InjectMocks
    private SysDefectStatisticServiceImpl sysDefectStatisticService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void participationMy_fillsMissingDaysWithZero() {
        when(sysDefectStatisticMapper.participationByDay(eq(100L), eq(1L), any()))
                .thenReturn(Collections.singletonList(new SysDefectParticipationDay("2026-05-31", 2)));

        List<SysDefectParticipationDay> list = sysDefectStatisticService.participationMy(100L, 1L, 30);

        assertEquals(30, list.size());
        long nonZero = list.stream().filter(d -> d.getCount() > 0).count();
        assertEquals(1, nonZero);
        assertTrue(list.stream().anyMatch(d -> d.getCount() == 2));
        verify(sysDefectStatisticMapper).participationByDay(eq(100L), eq(1L), any());
    }

    @Test
    public void participationMy_clampsDaysToRange() {
        assertEquals(14, SysDefectStatisticServiceImpl.clampParticipationDays(7));
        assertEquals(90, SysDefectStatisticServiceImpl.clampParticipationDays(120));
        assertEquals(30, SysDefectStatisticServiceImpl.clampParticipationDays(0));
    }

    @Test
    public void planMetrics_parsesPercentStringsToDouble() {
        SysPlan plan = new SysPlan();
        plan.setPlanId("p1");
        plan.setPlanName("Plan A");
        plan.setItemTotal(100);
        plan.setDefectCount(10);
        plan.setDefectCloseStateCount(5);
        plan.setCreateDefectCountByTester(8);
        plan.setCreateDefectCountByOutsider(2);
        plan.setDefectLevelUrgentCount(3);
        plan.setDefectHistoryPassCount(4);
        plan.setDefectRestartCount(1);
        when(sysPlanService.selectSysPlanList(any())).thenReturn(Collections.singletonList(plan));

        List<SysPlanMetricsItem> metrics = sysDefectStatisticService.planMetrics(100L);

        assertEquals(1, metrics.size());
        SysPlanMetricsItem item = metrics.get(0);
        assertEquals("p1", item.getPlanId());
        assertEquals(10.0, item.getDiscovery(), 0.01);
        assertEquals(50.0, item.getRepair(), 0.01);
        assertEquals(80.0, item.getDetection(), 0.01);
        assertEquals(30.0, item.getSeverity(), 0.01);
        assertEquals(25.0, item.getRestart(), 0.01);
        assertEquals(20.0, item.getEscape(), 0.01);
    }

    @Test
    public void planMetrics_returnsTopFourByUpdateTime() {
        List<SysPlan> plans = IntStream.rangeClosed(1, 6).mapToObj(i -> {
            SysPlan plan = new SysPlan();
            plan.setPlanId("p" + i);
            plan.setPlanName("Plan " + i);
            plan.setUpdateTime(new Date(i * 1000L));
            return plan;
        }).collect(java.util.stream.Collectors.toList());
        when(sysPlanService.selectSysPlanList(any())).thenReturn(plans);

        List<SysPlanMetricsItem> metrics = sysDefectStatisticService.planMetrics(100L);

        assertEquals(4, metrics.size());
        assertEquals("p6", metrics.get(0).getPlanId());
        assertEquals("p5", metrics.get(1).getPlanId());
        assertEquals("p4", metrics.get(2).getPlanId());
        assertEquals("p3", metrics.get(3).getPlanId());
    }

    @Test
    public void planMetrics_returnsZeroWhenNoDefects() {
        SysPlan plan = new SysPlan();
        plan.setPlanId("p2");
        plan.setPlanName("Empty");
        when(sysPlanService.selectSysPlanList(any())).thenReturn(Collections.singletonList(plan));

        SysPlanMetricsItem item = sysDefectStatisticService.planMetrics(100L).get(0);

        assertEquals(0.0, item.getDiscovery(), 0.01);
        assertEquals(0.0, item.getRepair(), 0.01);
        assertEquals(0.0, item.getDetection(), 0.01);
        assertEquals(0.0, item.getSeverity(), 0.01);
        assertEquals(0.0, item.getRestart(), 0.01);
        assertEquals(0.0, item.getEscape(), 0.01);
    }

    @Test
    public void planBurndownChart_delegatesToDashboardAndFillUtil() {
        SysPlan plan = new SysPlan();
        plan.setItemTotal(5);
        when(sysPlanService.selectSysPlanByPlanId("p1")).thenReturn(plan);
        when(sysDashboardService.planBurndown("p1")).thenReturn(Collections.emptyList());

        List<SysColumnsInChart> chart = sysDefectStatisticService.planBurndownChart("p1");

        assertEquals(31, chart.size());
        assertEquals(5, chart.get(0).getValue());
        verify(sysDashboardService).planBurndown("p1");
    }

    @Test
    public void planCountdownSummary_returnsCountFields() {
        SysPlan plan = new SysPlan();
        plan.setPlanId("p1");
        plan.setPlanName("Plan A");
        plan.setItemTotal(13);
        plan.setUnexecutedCount(3);
        plan.setPassCount(10);
        plan.setDefectCount(20);
        plan.setDefectCloseStateCount(15);
        when(sysPlanService.selectSysPlanByPlanId("p1")).thenReturn(plan);

        com.cat2bug.system.domain.SysPlanCountdownSummary summary =
                sysDefectStatisticService.planCountdownSummary("p1");

        assertNotNull(summary);
        assertEquals(13, summary.getItemTotal());
        assertEquals(10, summary.getPassCount());
        assertEquals(3, summary.getUnexecutedCount());
        assertEquals(20, summary.getDefectCount());
        assertEquals(15, summary.getDefectCloseStateCount());
    }

    @Test
    public void planList_returnsLightweightFields() {
        SysPlan plan = new SysPlan();
        plan.setPlanId("p1");
        plan.setPlanName("Plan A");
        when(sysPlanService.selectSysPlanList(any())).thenReturn(Collections.singletonList(plan));

        List<Map<String, Object>> list = sysDefectStatisticService.planList(100L);

        assertEquals(1, list.size());
        assertEquals("p1", list.get(0).get("planId"));
        assertEquals("Plan A", list.get(0).get("planName"));
    }

    @Test
    public void planBurndownUtil_fillsWhenRawEmpty() {
        SysPlan plan = new SysPlan();
        plan.setItemTotal(12);

        List<SysColumnsInChart> filled = PlanBurndownUtil.fillBurndown(Collections.emptyList(), plan);

        assertEquals(31, filled.size());
        assertTrue(filled.stream().allMatch(c -> c.getValue() == 12));
    }

    @Test
    public void parsePercent_stripsPercentSign() {
        assertEquals(12.34, SysDefectStatisticServiceImpl.parsePercent("12.34%"), 0.001);
        assertEquals(0.0, SysDefectStatisticServiceImpl.parsePercent("0%"), 0.001);
    }
}
