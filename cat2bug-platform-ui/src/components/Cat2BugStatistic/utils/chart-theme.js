/** 统计条 ECharts 深色/浅色主题片段 */
export function statisticChartColors(dark) {
  return {
    axisLabel: dark ? '#a8abb2' : '#909399',
    axisLine: dark ? '#4c4d4f' : '#dcdfe6',
    splitLine: dark ? '#363637' : '#ebeef5',
    tooltipBg: dark ? '#252529' : '#ffffff',
    tooltipBorder: dark ? '#363637' : '#ebeef5',
    tooltipText: dark ? '#e5eaf3' : '#303133',
    heatZero: dark ? '#2b2b2c' : '#ebedf0',
    heatLow: dark ? '#1e3a5f' : '#c6e2ff',
    heatMid: dark ? '#409eff' : '#79bbff',
    heatHigh: dark ? '#66b1ff' : '#337ecc',
    barBg: dark ? 'rgba(255,255,255,0.08)' : 'rgba(180,180,180,0.2)',
    radarSplit: dark
      ? ['rgba(255,255,255,0.03)', 'rgba(255,255,255,0.08)']
      : ['rgba(255,255,255,0.9)', 'rgba(228,231,237,0.9)'],
    radarLine: dark ? '#636466' : '#c0c4cc',
    cardBg: dark ? '#1d1e1f' : '#ffffff'
  }
}

/** 日历热力图（我的参与）— CSS 网格间隔背景 */
export function calendarHeatmapTheme(dark) {
  const base = statisticChartColors(dark)
  if (dark) {
    return {
      ...base,
      gridGap: '#252529',
      emptyCell: '#3a4048',
      heatActive: ['#1a3a5c', '#2563ab', '#409eff', '#79c0ff']
    }
  }
  return {
    ...base,
    gridGap: '#f8f8f9',
    emptyCell: '#ebedf0',
    heatActive: ['#c6e2ff', '#79bbff', '#409eff', '#337ecc']
  }
}
