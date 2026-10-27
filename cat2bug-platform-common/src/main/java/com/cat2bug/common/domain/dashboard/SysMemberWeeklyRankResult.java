package com.cat2bug.common.domain.dashboard;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作台「成员本周贡献」聚合响应：周界 + 按展示名排序的成员列表（无排名）。
 */
public class SysMemberWeeklyRankResult {

    private String weekStart;
    private String weekEnd;
    private List<SysMemberWeeklyRankItem> members = new ArrayList<>();

    public String getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(String weekStart) {
        this.weekStart = weekStart;
    }

    public String getWeekEnd() {
        return weekEnd;
    }

    public void setWeekEnd(String weekEnd) {
        this.weekEnd = weekEnd;
    }

    public List<SysMemberWeeklyRankItem> getMembers() {
        return members;
    }

    public void setMembers(List<SysMemberWeeklyRankItem> members) {
        this.members = members != null ? members : new ArrayList<>();
    }
}
