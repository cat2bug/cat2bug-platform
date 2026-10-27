package com.cat2bug.system.util;

import com.cat2bug.common.domain.dashboard.SysMemberWeeklyMetricRow;
import com.cat2bug.common.domain.dashboard.SysMemberWeeklyRankItem;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SysMemberWeeklyRankHelperTest {

    @Test
    void mergeAndSort_ordersByDisplayNameNotTotal() {
        SysMemberWeeklyMetricRow aDefect = row(1L, "Zoe", 2);
        SysMemberWeeklyMetricRow bDefect = row(2L, "Amy", 1);
        SysMemberWeeklyMetricRow aDoc = row(1L, "Zoe", 1);
        Map<Long, Integer> commits = new HashMap<>();
        commits.put(1L, 1);
        commits.put(2L, 1);

        List<SysMemberWeeklyRankItem> list = SysMemberWeeklyRankHelper.mergeAndSort(
                List.of(aDefect, bDefect), List.of(aDoc), commits);

        assertEquals(2, list.size());
        assertEquals(2L, list.get(0).getUserId());
        assertEquals(2, list.get(0).getTotal());
        assertEquals(1L, list.get(1).getUserId());
        assertEquals(4, list.get(1).getTotal());
    }

    @Test
    void mergeAndSort_omitsZeroTotal() {
        List<SysMemberWeeklyRankItem> list = SysMemberWeeklyRankHelper.mergeAndSort(
                List.of(), List.of(), Map.of());
        assertTrue(list.isEmpty());
    }

    @Test
    void mergeAndSort_emptyWeekListsNullSafe() {
        List<SysMemberWeeklyRankItem> list = SysMemberWeeklyRankHelper.mergeAndSort(null, null, null);
        assertTrue(list.isEmpty());
    }

    private static SysMemberWeeklyMetricRow row(Long userId, String nick, int count) {
        SysMemberWeeklyMetricRow r = new SysMemberWeeklyMetricRow();
        r.setUserId(userId);
        r.setNickName(nick);
        r.setUserName(nick);
        r.setMetricCount(count);
        return r;
    }
}
