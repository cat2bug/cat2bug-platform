/** ECharts 仪表盘图表：明/暗主题共用色板 */
export function getDashboardChartTheme(isDark) {
  return {
    legendText: isDark ? '#e5eaf3' : '#303133',
    legendInactive: isDark ? '#606266' : '#cccccc',
    axisColor: isDark ? '#909399' : '#606266',
    lineColor: isDark ? '#363637' : '#dcdfe6',
    splitColor: isDark ? '#2b2b2c' : '#ebeef5',
    tooltipBg: isDark ? '#252529' : '#ffffff',
    tooltipBorder: isDark ? '#363637' : '#ebeef5',
    tooltipText: isDark ? '#e5eaf3' : '#303133'
  }
}

export function buildDashboardAxisStyle(theme) {
  return {
    axisLabel: { color: theme.axisColor },
    axisLine: { lineStyle: { color: theme.lineColor } },
    axisTick: { lineStyle: { color: theme.lineColor } }
  }
}
