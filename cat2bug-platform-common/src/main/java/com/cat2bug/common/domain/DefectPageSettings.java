package com.cat2bug.common.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 团队缺陷页面设置（存 {@code sys_team.defect_page_config} JSON）。
 */
public class DefectPageSettings {

    public static final String VIEW_TABLE = "DefectTable";
    public static final String VIEW_EXCEL = "DefectExcel";
    public static final String VIEW_GANTT = "DefectGantt";
    public static final String VIEW_KANBAN = "DefectKanban";

    private Map<String, Boolean> views = new LinkedHashMap<>();
    private LifeTimeColors lifeTimeColors = new LifeTimeColors();
    private RejectCountColors rejectCountColors = new RejectCountColors();
    private boolean allowShare = true;

    public Map<String, Boolean> getViews() {
        return views;
    }

    public void setViews(Map<String, Boolean> views) {
        this.views = views;
    }

    public LifeTimeColors getLifeTimeColors() {
        return lifeTimeColors;
    }

    public void setLifeTimeColors(LifeTimeColors lifeTimeColors) {
        this.lifeTimeColors = lifeTimeColors;
    }

    public RejectCountColors getRejectCountColors() {
        return rejectCountColors;
    }

    public void setRejectCountColors(RejectCountColors rejectCountColors) {
        this.rejectCountColors = rejectCountColors;
    }

    public boolean isAllowShare() {
        return allowShare;
    }

    public void setAllowShare(boolean allowShare) {
        this.allowShare = allowShare;
    }

    public static class LifeTimeColors {
        private double greenBelowDays = 3;
        private double orangeBelowDays = 7;
        private String green = "#67C23A";
        private String orange = "#E6A23C";
        private String red = "#F56C6C";

        public double getGreenBelowDays() {
            return greenBelowDays;
        }

        public void setGreenBelowDays(double greenBelowDays) {
            this.greenBelowDays = greenBelowDays;
        }

        public double getOrangeBelowDays() {
            return orangeBelowDays;
        }

        public void setOrangeBelowDays(double orangeBelowDays) {
            this.orangeBelowDays = orangeBelowDays;
        }

        public String getGreen() {
            return green;
        }

        public void setGreen(String green) {
            this.green = green;
        }

        public String getOrange() {
            return orange;
        }

        public void setOrange(String orange) {
            this.orange = orange;
        }

        public String getRed() {
            return red;
        }

        public void setRed(String red) {
            this.red = red;
        }
    }

    public static class RejectCountColors {
        private int greenBelow = 3;
        private int orangeBelow = 7;
        private String green = "#67C23A";
        private String orange = "#E6A23C";
        private String red = "#F56C6C";

        public int getGreenBelow() {
            return greenBelow;
        }

        public void setGreenBelow(int greenBelow) {
            this.greenBelow = greenBelow;
        }

        public int getOrangeBelow() {
            return orangeBelow;
        }

        public void setOrangeBelow(int orangeBelow) {
            this.orangeBelow = orangeBelow;
        }

        public String getGreen() {
            return green;
        }

        public void setGreen(String green) {
            this.green = green;
        }

        public String getOrange() {
            return orange;
        }

        public void setOrange(String orange) {
            this.orange = orange;
        }

        public String getRed() {
            return red;
        }

        public void setRed(String red) {
            this.red = red;
        }
    }
}
