package com.cat2bug.common.domain.dashboard;

/**
 * 管理工作台「本周运营」汇总：角色缺陷动作 + 渠道调用次数。
 */
public class SysWeekOpsStatistics {

    private long testerCreated;
    private long testerClosed;
    private long developerRepaired;
    private long outsiderCreated;
    private long mcpCallCount;
    private long apiCallCount;
    private long llmCallCount;
    private String weekStart;
    private String weekEnd;

    public long getTesterCreated() {
        return testerCreated;
    }

    public void setTesterCreated(long testerCreated) {
        this.testerCreated = testerCreated;
    }

    public long getTesterClosed() {
        return testerClosed;
    }

    public void setTesterClosed(long testerClosed) {
        this.testerClosed = testerClosed;
    }

    public long getDeveloperRepaired() {
        return developerRepaired;
    }

    public void setDeveloperRepaired(long developerRepaired) {
        this.developerRepaired = developerRepaired;
    }

    public long getOutsiderCreated() {
        return outsiderCreated;
    }

    public void setOutsiderCreated(long outsiderCreated) {
        this.outsiderCreated = outsiderCreated;
    }

    public long getMcpCallCount() {
        return mcpCallCount;
    }

    public void setMcpCallCount(long mcpCallCount) {
        this.mcpCallCount = mcpCallCount;
    }

    public long getApiCallCount() {
        return apiCallCount;
    }

    public void setApiCallCount(long apiCallCount) {
        this.apiCallCount = apiCallCount;
    }

    public long getLlmCallCount() {
        return llmCallCount;
    }

    public void setLlmCallCount(long llmCallCount) {
        this.llmCallCount = llmCallCount;
    }

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
}
