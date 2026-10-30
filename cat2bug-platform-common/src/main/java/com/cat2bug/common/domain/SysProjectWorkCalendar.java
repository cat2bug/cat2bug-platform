package com.cat2bug.common.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目工作日历 sys_project_work_calendar。
 * workWeek：周一=1 … 周日=7；exceptions：YYYY-MM-DD → holiday|workday。
 */
public class SysProjectWorkCalendar {

    public static final String EXCEPTION_HOLIDAY = "holiday";
    public static final String EXCEPTION_WORKDAY = "workday";

    private Long projectId;
    /** 工作周，默认周一至周五 */
    private List<Integer> workWeek = defaultWorkWeek();
    private Map<String, String> exceptions = new LinkedHashMap<>();
    private String updateBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public static List<Integer> defaultWorkWeek() {
        List<Integer> week = new ArrayList<>(5);
        week.add(1);
        week.add(2);
        week.add(3);
        week.add(4);
        week.add(5);
        return week;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public List<Integer> getWorkWeek() {
        return workWeek;
    }

    public void setWorkWeek(List<Integer> workWeek) {
        this.workWeek = workWeek != null ? workWeek : defaultWorkWeek();
    }

    public Map<String, String> getExceptions() {
        return exceptions;
    }

    public void setExceptions(Map<String, String> exceptions) {
        this.exceptions = exceptions != null ? exceptions : new LinkedHashMap<>();
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
