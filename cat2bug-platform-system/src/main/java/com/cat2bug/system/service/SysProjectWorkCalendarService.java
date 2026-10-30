package com.cat2bug.system.service;

import com.cat2bug.common.domain.SysProjectWorkCalendar;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.system.mapper.SysProjectWorkCalendarMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 项目工作日历：默认周一～五；例外 holiday / workday。
 */
@ApplicationScoped
public class SysProjectWorkCalendarService {

    private static final Pattern DATE_KEY = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    @Inject
    SysProjectWorkCalendarMapper mapper;

    public SysProjectWorkCalendar getOrDefault(Long projectId) {
        SysProjectWorkCalendar row = projectId == null ? null : mapper.selectByProjectId(projectId);
        if (row == null) {
            row = new SysProjectWorkCalendar();
            row.setProjectId(projectId);
            row.setWorkWeek(SysProjectWorkCalendar.defaultWorkWeek());
            row.setExceptions(new LinkedHashMap<>());
            return row;
        }
        row.setWorkWeek(normalizeWorkWeek(row.getWorkWeek()));
        row.setExceptions(normalizeExceptions(row.getExceptions()));
        return row;
    }

    public SysProjectWorkCalendar save(Long projectId, List<Integer> workWeek, Map<String, String> exceptions,
                                       String updateBy) {
        if (projectId == null) {
            throw new ApiException(400, "projectId required");
        }
        SysProjectWorkCalendar row = new SysProjectWorkCalendar();
        row.setProjectId(projectId);
        row.setWorkWeek(normalizeWorkWeek(workWeek));
        row.setExceptions(normalizeExceptions(exceptions));
        row.setUpdateBy(updateBy);
        row.setUpdateTime(new Date());
        if (mapper.update(row) == 0) {
            mapper.insert(row);
        }
        return getOrDefault(projectId);
    }

    static List<Integer> normalizeWorkWeek(List<Integer> workWeek) {
        if (workWeek == null || workWeek.isEmpty()) {
            return SysProjectWorkCalendar.defaultWorkWeek();
        }
        LinkedHashSet<Integer> set = new LinkedHashSet<>();
        for (Integer d : workWeek) {
            if (d != null && d >= 1 && d <= 7) {
                set.add(d);
            }
        }
        if (set.isEmpty()) {
            return SysProjectWorkCalendar.defaultWorkWeek();
        }
        return new ArrayList<>(set);
    }

    static Map<String, String> normalizeExceptions(Map<String, String> exceptions) {
        Map<String, String> out = new LinkedHashMap<>();
        if (exceptions == null) {
            return out;
        }
        for (Map.Entry<String, String> e : exceptions.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) {
                continue;
            }
            String key = e.getKey().trim();
            String val = e.getValue().trim().toLowerCase();
            if (!DATE_KEY.matcher(key).matches() || !isExceptionType(val)) {
                continue;
            }
            out.put(key, val);
        }
        return out;
    }

    private static boolean isExceptionType(String val) {
        return SysProjectWorkCalendar.EXCEPTION_HOLIDAY.equals(val)
                || SysProjectWorkCalendar.EXCEPTION_WORKDAY.equals(val);
    }
}
