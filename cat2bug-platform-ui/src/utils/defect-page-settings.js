/** 团队缺陷页面设置：默认值、视图降级、色阶 */

export const DEFECT_VIEW_ORDER = Object.freeze([
  'DefectTable',
  'DefectExcel',
  'DefectGantt',
  'DefectKanban'
])

export function defaultDefectPageSettings() {
  return {
    views: {
      DefectTable: true,
      DefectExcel: true,
      DefectGantt: true,
      DefectKanban: true
    },
    lifeTimeColors: {
      greenBelowDays: 3,
      orangeBelowDays: 7,
      green: '#67C23A',
      orange: '#E6A23C',
      red: '#F56C6C'
    },
    rejectCountColors: {
      greenBelow: 3,
      orangeBelow: 7,
      green: '#67C23A',
      orange: '#E6A23C',
      red: '#F56C6C'
    },
    allowShare: true
  }
}

export function mergeDefectPageSettings(raw) {
  const base = defaultDefectPageSettings()
  if (!raw || typeof raw !== 'object') {
    return base
  }
  const views = { ...base.views }
  if (raw.views && typeof raw.views === 'object') {
    DEFECT_VIEW_ORDER.forEach((key) => {
      if (typeof raw.views[key] === 'boolean') {
        views[key] = raw.views[key]
      }
    })
  }
  return {
    views,
    lifeTimeColors: { ...base.lifeTimeColors, ...(raw.lifeTimeColors || {}) },
    rejectCountColors: { ...base.rejectCountColors, ...(raw.rejectCountColors || {}) },
    allowShare: raw.allowShare !== false
  }
}

export function enabledDefectViews(settings) {
  const s = mergeDefectPageSettings(settings)
  return DEFECT_VIEW_ORDER.filter((key) => s.views[key])
}

/** 缓存视图非法时降级：优先表格，否则启用列表第一项 */
export function resolveDefectContentView(cached, settings) {
  const enabled = enabledDefectViews(settings)
  if (!enabled.length) {
    return 'DefectTable'
  }
  if (cached && enabled.includes(cached)) {
    return cached
  }
  if (enabled.includes('DefectTable')) {
    return 'DefectTable'
  }
  return enabled[0]
}

export function lifeTimeMetricColor(settings, lifeMs) {
  const c = mergeDefectPageSettings(settings).lifeTimeColors
  const days = Number(lifeMs) / (1000 * 60 * 60 * 24)
  if (days < Number(c.greenBelowDays)) return c.green
  if (days < Number(c.orangeBelowDays)) return c.orange
  return c.red
}

export function rejectCountMetricColor(settings, rejectCount) {
  const c = mergeDefectPageSettings(settings).rejectCountColors
  const n = Number(rejectCount) || 0
  if (n < Number(c.greenBelow)) return c.green
  if (n < Number(c.orangeBelow)) return c.orange
  return c.red
}

/** 从缺陷对象估算存活毫秒（与 lifeTime 起止一致） */
export function defectLifeMs(defect) {
  if (!defect) return 0
  const startSource = defect.handleStartTime || defect.createTime
  if (!startSource) return 0
  const start = new Date(startSource).getTime()
  const end = defect.handlePassTime
    ? new Date(defect.handlePassTime).getTime()
    : Date.now()
  if (Number.isNaN(start) || Number.isNaN(end)) return 0
  return Math.max(0, end - start)
}
