/** 工作台二期自由栅格：12 列，layout 项 { i, x, y, w, h } */

export const GRID_COLS = 12
export const GRID_MIN_W = 2
export const GRID_MIN_H = 2
/** 与 CSS --wb-grid-row-h 对齐（px），用于拖拽换算 */
export const GRID_ROW_H = 48
export const GRID_GAP = 12
/** 小屏断点：低于此宽度回退纵向排序（内容区宽度，含侧栏时主区常 <960） */
export const GRID_STACK_MQ = 720

/** 保序去重 */
export function dedupeIds(ids) {
  const seen = new Set()
  const out = []
  for (const id of ids || []) {
    if (!id || seen.has(id)) continue
    seen.add(id)
    out.push(id)
  }
  return out
}

/**
 * 按 widgets 顺序装箱生成 layout
 * @param {string[]} widgetIds
 * @param {(id: string) => { w?: number, h?: number }} sizeOf
 */
export function packLayout(widgetIds, sizeOf) {
  let x = 0
  let y = 0
  let rowH = 0
  const out = []
  for (const id of dedupeIds(widgetIds)) {
    const sz = (sizeOf && sizeOf(id)) || {}
    const w = clamp(Number(sz.w) || 6, GRID_MIN_W, GRID_COLS)
    const h = Math.max(GRID_MIN_H, Number(sz.h) || 4)
    if (x + w > GRID_COLS) {
      x = 0
      y += rowH
      rowH = 0
    }
    out.push({ i: id, x, y, w, h })
    x += w
    rowH = Math.max(rowH, h)
  }
  return out
}

/**
 * 对齐 layout 与 widgets：补缺、去未知、夹紧尺寸（layout 按 id 唯一）
 */
export function syncLayout(layout, widgetIds, sizeOf) {
  const ids = dedupeIds(widgetIds)
  const byId = new Map()
  for (const item of Array.isArray(layout) ? layout : []) {
    if (!item || !item.i || byId.has(item.i)) continue
    byId.set(item.i, sanitizeItem(item))
  }
  const kept = []
  for (const id of ids) {
    if (byId.has(id)) {
      kept.push(byId.get(id))
    } else {
      const sz = (sizeOf && sizeOf(id)) || {}
      kept.push({
        i: id,
        x: 0,
        y: layoutMaxY(kept),
        w: clamp(Number(sz.w) || 6, GRID_MIN_W, GRID_COLS),
        h: Math.max(GRID_MIN_H, Number(sz.h) || 4)
      })
    }
  }
  return kept
}

export function layoutMaxY(layout) {
  let max = 0
  for (const item of layout || []) {
    max = Math.max(max, (Number(item.y) || 0) + (Number(item.h) || 0))
  }
  return max
}

/**
 * 按列数均分排版（默认 3 列 → 每块 w=4，左/中/右）
 * @param {string[]} widgetIds
 * @param {number} cols 2～6
 * @param {(id: string) => { h?: number }} sizeOf
 */
export function layoutAsEqualColumns(widgetIds, cols = 3, sizeOf) {
  const n = clamp(Number(cols) || 3, 2, 6)
  const cellW = Math.floor(GRID_COLS / n)
  const ids = dedupeIds(widgetIds)
  const out = []
  ids.forEach((id, i) => {
    const col = i % n
    const row = Math.floor(i / n)
    const sz = (sizeOf && sizeOf(id)) || {}
    const h = Math.max(GRID_MIN_H, Number(sz.h) || 4)
    // 同一行取该行最大高度，避免矮块把下一行顶乱：先占位，第二遍对齐行高
    out.push({ i: id, x: col * cellW, y: 0, w: cellW, h, _row: row })
  })
  const rowH = new Map()
  for (const it of out) {
    rowH.set(it._row, Math.max(rowH.get(it._row) || 0, it.h))
  }
  let y = 0
  const yOfRow = new Map()
  const rows = [...rowH.keys()].sort((a, b) => a - b)
  for (const r of rows) {
    yOfRow.set(r, y)
    y += rowH.get(r)
  }
  return out.map(({ _row, ...it }) => ({
    ...it,
    y: yOfRow.get(_row) || 0,
    h: rowH.get(_row) || it.h
  }))
}

/** 小屏纵向顺序：先 y 后 x */
export function sortLayoutForStack(layout) {
  return [...(layout || [])].sort((a, b) => {
    const dy = (Number(a.y) || 0) - (Number(b.y) || 0)
    if (dy !== 0) return dy
    return (Number(a.x) || 0) - (Number(b.x) || 0)
  })
}

export function hasUsableLayout(tabConfig) {
  return Array.isArray(tabConfig?.layout) && tabConfig.layout.length > 0
}

export function configUsesFreeGrid(config) {
  return Number(config?.version) >= 2
}

/**
 * 进入自由栅格编辑：升 version=2，保证当前页签有 layout
 */
/**
 * 管理 · 工作台：对齐一期分区（周报+类型KPI / 趋势+饼图 / 待办+右侧栈）
 * 避免通用 pack 把 PR 顶到第一行、三列均分打散结构
 */
export function packManagerWorkLayout(widgetIds, sizeOf) {
  const ids = dedupeIds(widgetIds).filter(id => id && id !== 'quick-create')
  const set = new Set(ids)
  const has = (id) => set.has(id)
  const hOf = (id, fallback) => Math.max(GRID_MIN_H, Number((sizeOf && sizeOf(id)?.h) || fallback))
  const items = []
  let y = 0

  // 顶：项目周报 | 任务·需求·BUG（等高，KPI 内多指标填满）
  const weeklyH = has('team-weekly-report') ? hOf('team-weekly-report', 5) : 0
  const typeKpiH = has('team-type-kpi') ? hOf('team-type-kpi', 5) : 0
  const topH = Math.max(weeklyH, typeKpiH, 5)
  if (has('team-weekly-report') && has('team-type-kpi')) {
    items.push({ i: 'team-weekly-report', x: 0, y, w: 4, h: topH })
    items.push({ i: 'team-type-kpi', x: 4, y, w: 8, h: topH })
    y += topH
  } else if (has('team-weekly-report')) {
    items.push({ i: 'team-weekly-report', x: 0, y, w: 12, h: weeklyH })
    y += weeklyH
  } else if (has('team-type-kpi')) {
    items.push({ i: 'team-type-kpi', x: 0, y, w: 12, h: typeKpiH })
    y += typeKpiH
  }

  // 中：趋势 | 计划燃尽（取代旧缺陷状态饼图）
  const rightChartId = has('plan-burndown')
    ? 'plan-burndown'
    : (has('team-workload-mix') ? 'team-workload-mix' : null)
  const chartH = Math.max(
    has('team-defect-trend') ? hOf('team-defect-trend', 4) : 0,
    rightChartId ? hOf(rightChartId, 4) : 0
  )
  if (has('team-defect-trend') && rightChartId) {
    items.push({ i: 'team-defect-trend', x: 0, y, w: 7, h: chartH })
    items.push({ i: rightChartId, x: 7, y, w: 5, h: chartH })
    y += chartH
  } else if (has('team-defect-trend')) {
    items.push({ i: 'team-defect-trend', x: 0, y, w: 12, h: hOf('team-defect-trend', 4) })
    y += hOf('team-defect-trend', 4)
  } else if (rightChartId) {
    items.push({ i: rightChartId, x: 0, y, w: 12, h: hOf(rightChartId, 4) })
    y += hOf(rightChartId, 4)
  }

  // 下：项目待办 | 计划 / 评审 / 源码 / PR
  const stackOrder = [
    'case-pending-review',
    'document-list',
    'git-repo-stats',
    'recent-commits'
  ].filter(has)
  const hasLeft = has('team-defects')
  if (hasLeft || stackOrder.length) {
    let stackH = 0
    for (const id of stackOrder) stackH += hOf(id, 3)
    const leftH = hasLeft ? Math.max(hOf('team-defects', 6), stackH || 6) : 0
    if (hasLeft) {
      items.push({
        i: 'team-defects',
        x: 0,
        y,
        w: stackOrder.length ? 7 : 12,
        h: Math.max(leftH, 6)
      })
    }
    let sy = y
    const sx = hasLeft ? 7 : 0
    const sw = hasLeft ? 5 : 12
    for (const id of stackOrder) {
      const h = hOf(id, 3)
      items.push({ i: id, x: sx, y: sy, w: sw, h })
      sy += h
    }
    y += Math.max(leftH, stackH)
  }

  // 其余未覆盖的组件（若有）追加
  const placed = new Set(items.map(it => it.i))
  const rest = ids.filter(id => !placed.has(id))
  if (rest.length) {
    const packed = packLayout(rest, sizeOf).map(it => ({ ...it, y: it.y + y }))
    items.push(...packed)
  }
  return items
}

/** 管理布局是否明显错乱（如 PR 压在顶行而周报/KPI 不在顶行） */
export function managerWorkLayoutBroken(layout, widgetIds) {
  const ids = new Set(dedupeIds(widgetIds))
  const items = Array.isArray(layout) ? layout : []
  if (!items.length) return true
  const byId = new Map(items.map(it => [it.i, it]))
  // 缺关键块
  for (const need of ['team-weekly-report', 'team-type-kpi']) {
    if (ids.has(need) && !byId.has(need)) return true
  }
  const weekly = byId.get('team-weekly-report')
  const kpi = byId.get('team-type-kpi')
  const trend = byId.get('team-defect-trend')
  // 周报 / 类型 KPI 必须在顶行
  if (weekly && (weekly.y || 0) > 1) return true
  if (kpi && (kpi.y || 0) > 1) return true
  // 周报与类型 KPI 应同顶行
  if (weekly && kpi && Math.abs((weekly.y || 0) - (kpi.y || 0)) > 1) return true
  // 趋势不应与顶区同排（三列均分会把趋势挤到 y=0）
  if (trend && weekly && (trend.y || 0) <= (weekly.y || 0)) return true
  // 顶行只允许周报 + 类型 KPI（其余出现在顶行即视为错乱）
  if (weekly || kpi) {
    const topY = Math.min(
      ...( [weekly, kpi].filter(Boolean).map(it => Number(it.y) || 0) )
    )
    const allowedTop = new Set(['team-weekly-report', 'team-type-kpi'])
    for (const it of items) {
      if ((Number(it.y) || 0) <= topY && !allowedTop.has(it.i)) return true
    }
  }
  // 重叠检测（矩形相交）
  for (let a = 0; a < items.length; a++) {
    for (let b = a + 1; b < items.length; b++) {
      if (rectsOverlap(items[a], items[b])) return true
    }
  }
  return false
}

function rectsOverlap(a, b) {
  const ax1 = Number(a.x) || 0
  const ay1 = Number(a.y) || 0
  const ax2 = ax1 + (Number(a.w) || 0)
  const ay2 = ay1 + (Number(a.h) || 0)
  const bx1 = Number(b.x) || 0
  const by1 = Number(b.y) || 0
  const bx2 = bx1 + (Number(b.w) || 0)
  const by2 = by1 + (Number(b.h) || 0)
  return ax1 < bx2 && ax2 > bx1 && ay1 < by2 && ay2 > by1
}

/**
 * @param {object} config
 * @param {string} tab
 * @param {(id: string) => { w?: number, h?: number }} sizeOf
 * @param {(ids: string[], tab: string) => string[]} [resolveVisible] 过滤 toolbar/无权限后的可见 id
 * @param {{ preset?: string, forceRepack?: boolean }} [opts]
 */
export function ensureTabLayout(config, tab, sizeOf, resolveVisible, opts = {}) {
  const next = JSON.parse(JSON.stringify(config || {}))
  next.version = Math.max(2, Number(next.version) || 1)
  if (!next.tabs) next.tabs = {}
  if (!next.tabs[tab]) next.tabs[tab] = { widgets: [] }
  const tabCfg = next.tabs[tab]
  tabCfg.widgets = dedupeIds(tabCfg.widgets || [])
  // 别名合并：旧 prs-to-review → open-pull-requests
  tabCfg.widgets = tabCfg.widgets.map(id => (id === 'prs-to-review' ? 'open-pull-requests' : id))
  tabCfg.widgets = dedupeIds(tabCfg.widgets)
  const forLayout = typeof resolveVisible === 'function'
    ? resolveVisible(tabCfg.widgets, tab)
    : tabCfg.widgets.filter(id => id !== 'quick-create')

  const preset = opts.preset || next.preset
  const useManagerPack = preset === 'manager' && tab === 'work'
  const broken = useManagerPack && managerWorkLayoutBroken(tabCfg.layout, forLayout)
  const needPack = opts.forceRepack || !hasUsableLayout(tabCfg) || broken

  if (needPack) {
    tabCfg.layout = useManagerPack
      ? packManagerWorkLayout(forLayout, sizeOf)
      : packLayout(forLayout, sizeOf)
  } else {
    tabCfg.layout = syncLayout(tabCfg.layout, forLayout, sizeOf)
  }
  return next
}

function sanitizeItem(raw) {
  const w = clamp(Number(raw.w) || 6, GRID_MIN_W, GRID_COLS)
  const h = Math.max(GRID_MIN_H, Number(raw.h) || 4)
  let x = Math.max(0, Number(raw.x) || 0)
  if (x + w > GRID_COLS) x = Math.max(0, GRID_COLS - w)
  const y = Math.max(0, Number(raw.y) || 0)
  return { i: String(raw.i), x, y, w, h }
}

function clamp(n, min, max) {
  return Math.min(max, Math.max(min, n))
}
