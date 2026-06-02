import { statisticChartColors } from './chart-theme'

/** SysDefectStateEnum 序 → 缺陷列表 defectStates 参数 */
export const DEFECT_STATE_NAME_TO_ID = {
  PROCESSING: '0',
  AUDIT: '1',
  RESOLVED: '2',
  REJECTED: '3',
  CLOSED: '4'
}

export function buildDefectStateLineSeries(apiData, translate) {
  const series = []
  const types = apiData.types || []
  types.forEach(k => {
    const key = typeof k === 'string' ? k : (k && (k.name || k.toString()))
    if (!apiData.data || !apiData.data[key]) return
    series.push({
      name: translate(key),
      type: 'line',
      data: apiData.data[key],
      _stateKey: key
    })
  })
  return series
}

/** 切换语言后按 _stateKey 重译图例/系列名 */
export function refreshDefectStateSeriesI18n(series, translate) {
  if (!Array.isArray(series)) return
  series.forEach(item => {
    if (item._stateKey) {
      item.name = translate(item._stateKey)
    }
  })
}

export function buildMemberDefectLineSeries(apiData) {
  const rows = apiData.data || []
  const times = apiData.time || []
  const memberKeys = [...new Set(rows.filter(d => d.userId != null).map(d => String(d.userId)))]
  return memberKeys.map(userKey => {
    const sample = rows.find(d => String(d.userId) === userKey)
    const nick = sample ? (sample.nickName || sample.userName || userKey) : userKey
    const userId = sample ? sample.userId : null
    const data = times.map(t => {
      const val = rows.find(d => String(d.userId) === userKey && d.createTime === t)
      return val ? val.defectTodayCount : 0
    })
    return {
      name: nick,
      type: 'line',
      data,
      _userId: userId
    }
  })
}

export function buildTrendLineChartOption({ legendData, xData, series, isDark, tooltipAppendTo }) {
  const colors = statisticChartColors(isDark)
  const tooltip = {
    trigger: 'axis',
    confine: true,
    extraCssText: 'z-index: 5000;',
    backgroundColor: colors.tooltipBg,
    borderColor: colors.tooltipBorder,
    textStyle: { color: colors.tooltipText, fontSize: 11 }
  }
  if (tooltipAppendTo) {
    tooltip.appendTo = tooltipAppendTo
  }
  return {
    tooltip,
    legend: {
      type: 'scroll',
      top: 0,
      left: 0,
      right: 2,
      padding: [0, 0, 0, 0],
      itemWidth: 8,
      itemHeight: 6,
      itemGap: 4,
      textStyle: { color: colors.axisLabel, fontSize: 9, lineHeight: 10 },
      data: legendData
    },
    grid: {
      top: 12,
      left: 0,
      right: 4,
      bottom: 3,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xData,
      axisLabel: {
        color: colors.axisLabel,
        fontSize: 8,
        margin: 5,
        interval: 'auto'
      },
      axisLine: { lineStyle: { color: colors.axisLine } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: {
        color: colors.axisLabel,
        fontSize: 8,
        margin: 2,
        width: 24,
        showMaxLabel: false
      },
      axisLine: { show: false },
      splitLine: { lineStyle: { color: colors.splitLine } }
    },
    series
  }
}

export function formatUpdateTimeRange(timeLabel, timeType) {
  if (!timeLabel) return { beginUpdateTime: null, endUpdateTime: null }
  if (timeType === 'month') {
    const [y, m] = timeLabel.split('-').map(Number)
    const lastDay = new Date(y, m, 0).getDate()
    const mm = String(m).padStart(2, '0')
    return {
      beginUpdateTime: `${y}-${mm}-01 00:00:00`,
      endUpdateTime: `${y}-${mm}-${String(lastDay).padStart(2, '0')} 23:59:59`
    }
  }
  return {
    beginUpdateTime: `${timeLabel} 00:00:00`,
    endUpdateTime: `${timeLabel} 23:59:59`
  }
}
