package com.cat2bug.quarkus.common.domain.dashboard;

public class SysMemberStatistics {

    private int total;
    private int adminCount;
    private int testCount;
    private int developmentCount;
    private int outsideCount;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getAdminCount() {
        return adminCount;
    }

    public void setAdminCount(int adminCount) {
        this.adminCount = adminCount;
    }

    public int getTestCount() {
        return testCount;
    }

    public void setTestCount(int testCount) {
        this.testCount = testCount;
    }

    public int getDevelopmentCount() {
        return developmentCount;
    }

    public void setDevelopmentCount(int developmentCount) {
        this.developmentCount = developmentCount;
    }

    public int getOutsideCount() {
        return outsideCount;
    }

    public void setOutsideCount(int outsideCount) {
        this.outsideCount = outsideCount;
    }
}
