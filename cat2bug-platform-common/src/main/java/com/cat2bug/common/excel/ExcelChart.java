package com.cat2bug.common.excel;

import java.util.List;

/**
 * 嵌入 xlsx 的原生图表。锚点为双格锚点（左上 col1,row1 到右下 col2,row2，从 0 开始，右下为不含边界）。
 * <ul>
 *   <li>{@link Type#LINE}：平滑折线 + 圆点标记。</li>
 *   <li>{@link Type#BAR}：簇状竖向柱图，数值标签在柱外顶端。</li>
 * </ul>
 * 序列颜色按顺序取 {@link #ECHARTS_COLORS}，线宽 2 磅，与前端 ECharts 配色一致。
 */
public record ExcelChart(Type type, String title, int col1, int row1, int col2, int row2,
                         String valueAxisTitle, Legend legend, List<ExcelChartSeries> series) {

    public static final List<String> ECHARTS_COLORS = List.of(
            "2EC7C9", "B6A2DE", "5AB1EF", "FFB980", "D87A80", "8D98B3", "E5CF0D", "97B552");

    public enum Type { LINE, BAR }

    public enum Legend { NONE, TOP, RIGHT }

    public ExcelChart {
        series = List.copyOf(series);
    }
}
