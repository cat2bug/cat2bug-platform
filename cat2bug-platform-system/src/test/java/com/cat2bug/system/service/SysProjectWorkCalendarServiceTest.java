package com.cat2bug.system.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SysProjectWorkCalendarServiceTest {

    @Test
    void normalizeWorkWeekDefaultsAndFilters() {
        assertEquals(List.of(1, 2, 3, 4, 5), SysProjectWorkCalendarService.normalizeWorkWeek(null));
        assertEquals(List.of(1, 2, 3, 4, 5), SysProjectWorkCalendarService.normalizeWorkWeek(List.of()));
        assertEquals(List.of(1, 6), SysProjectWorkCalendarService.normalizeWorkWeek(List.of(1, 6, 9, 0)));
    }

    @Test
    void normalizeExceptionsKeepsHolidayAndWorkday() {
        Map<String, String> raw = Map.of(
                "2026-10-01", "holiday",
                "2026-09-27", "workday",
                "bad", "holiday",
                "2026-10-02", "other");
        Map<String, String> out = SysProjectWorkCalendarService.normalizeExceptions(raw);
        assertEquals("holiday", out.get("2026-10-01"));
        assertEquals("workday", out.get("2026-09-27"));
        assertFalse(out.containsKey("bad"));
        assertFalse(out.containsKey("2026-10-02"));
    }
}
