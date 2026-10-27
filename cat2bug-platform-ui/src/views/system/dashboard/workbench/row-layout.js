/** 工作台三期：行 + 列槽位配置（version: 3）
 * 列宽按 24 栅格：每行列 span 为正整数且总和恒为 24。
 */

export const CONFIG_VERSION = 3
export const MAX_COLS_PER_ROW = 4
export const GRID_SPAN = 24
/** 单列最少占格（拖拽/归一化下限） */
export const MIN_COL_SPAN = 2
export const HEIGHT_MODES = ['content', 'row', 'fixed']
export const FIXED_HEIGHT_PRESETS = { S: 160, M: 240, L: 320 }

let _seq = 0
export function newId(prefix = 'id') {
  _seq += 1
  return `${prefix}-${Date.now().toString(36)}-${_seq}`
}

export function normalizeHeight(raw, fallbackMode = 'row') {
  if (!raw || typeof raw !== 'object') {
    return { mode: fallbackMode }
  }
  const mode = HEIGHT_MODES.includes(raw.mode) ? raw.mode : fallbackMode
  if (mode === 'fixed') {
    const px = Math.max(80, Number(raw.px) || FIXED_HEIGHT_PRESETS.M)
    return { mode: 'fixed', px }
  }
  return { mode }
}

export function normalizeWidget(raw) {
  if (!raw || typeof raw !== 'object' || !raw.type) return null
  return {
    instanceId: String(raw.instanceId || newId('i')),
    type: String(raw.type),
    props: raw.props && typeof raw.props === 'object' ? { ...raw.props } : {}
  }
}

/** 等分 n 列，总和 GRID_SPAN */
export function equalSpans(n) {
  const count = Math.min(MAX_COLS_PER_ROW, Math.max(1, Number(n) || 1))
  const base = Math.floor(GRID_SPAN / count)
  const rem = GRID_SPAN - base * count
  return Array.from({ length: count }, (_, i) => base + (i < rem ? 1 : 0))
}

/**
 * 任意相对比例 → 正整数 span，总和 24，每列 ≥ MIN_COL_SPAN
 */
export function normalizeSpans(ratios) {
  const n = (ratios || []).length
  if (!n) return []
  if (n === 1) return [GRID_SPAN]
  const nums = ratios.map(r => Math.max(0, Number(r) || 0))
  const sum = nums.reduce((a, b) => a + b, 0)
  if (sum <= 0) return equalSpans(n)

  const exact = nums.map(x => (x / sum) * GRID_SPAN)
  const floors = exact.map(x => Math.floor(x))
  let used = floors.reduce((a, b) => a + b, 0)
  const order = exact
    .map((x, i) => ({ i, frac: x - floors[i] }))
    .sort((a, b) => b.frac - a.frac)
  const out = floors.slice()
  let k = 0
  while (used < GRID_SPAN && k < GRID_SPAN * 2) {
    out[order[k % n].i] += 1
    used += 1
    k += 1
  }

  // 抬升低于下限的列，从最大列借
  for (let guard = 0; guard < GRID_SPAN; guard++) {
    const low = out.findIndex(v => v < MIN_COL_SPAN)
    if (low < 0) break
    const need = MIN_COL_SPAN - out[low]
    let donor = -1
    let donorVal = -1
    out.forEach((v, i) => {
      if (i !== low && v - need >= MIN_COL_SPAN && v > donorVal) {
        donor = i
        donorVal = v
      }
    })
    if (donor < 0) break
    out[low] += need
    out[donor] -= need
  }

  // 最终校正总和
  let total = out.reduce((a, b) => a + b, 0)
  if (total !== GRID_SPAN) {
    const diff = GRID_SPAN - total
    const idx = out.indexOf(Math.max(...out))
    out[idx] = Math.max(MIN_COL_SPAN, out[idx] + diff)
  }
  return out
}

export function spansLabel(spans) {
  return (spans || []).join(':')
}

export function parseSpansLabel(label) {
  return String(label || '')
    .split(/[:：]/)
    .map(x => Number(x))
    .filter(x => Number.isFinite(x) && x > 0)
}

/** 当前列数可选的 24 栅格比例（下拉） */
export function ratioPresetOptions(colCount) {
  const n = Math.min(MAX_COLS_PER_ROW, Math.max(1, Number(colCount) || 1))
  const table = {
    1: [[24]],
    2: [
      [12, 12],
      [8, 16],
      [16, 8],
      [6, 18],
      [18, 6],
      [9, 15],
      [15, 9],
      [10, 14],
      [14, 10],
      [4, 20],
      [20, 4]
    ],
    3: [
      [8, 8, 8],
      [12, 6, 6],
      [6, 12, 6],
      [6, 6, 12],
      [10, 8, 6],
      [6, 8, 10],
      [12, 8, 4],
      [4, 8, 12],
      [10, 10, 4],
      [4, 10, 10],
      [14, 5, 5],
      [5, 14, 5],
      [5, 5, 14]
    ],
    4: [
      [6, 6, 6, 6],
      [8, 8, 4, 4],
      [4, 4, 8, 8],
      [12, 4, 4, 4],
      [4, 4, 4, 12],
      [8, 6, 6, 4],
      [4, 6, 6, 8],
      [9, 5, 5, 5],
      [5, 5, 5, 9],
      [10, 6, 4, 4],
      [4, 4, 6, 10]
    ]
  }
  return (table[n] || []).map(spans => ({
    value: spansLabel(spans),
    label: spansLabel(spans),
    spans
  }))
}

/** 下拉选项：预设 + 当前值（若不在预设中） */
export function ratioSelectOptions(colCount, currentLabel) {
  const presets = ratioPresetOptions(colCount)
  const seen = new Set(presets.map(p => p.value))
  const cur = String(currentLabel || '')
  const spans = parseSpansLabel(cur)
  if (cur && spans.length === colCount && !seen.has(cur)) {
    const normalized = normalizeSpans(spans)
    const value = spansLabel(normalized)
    if (!seen.has(value)) {
      presets.unshift({ value, label: value, spans: normalized })
    }
  }
  return presets
}

export function normalizeCol(raw) {
  const ratio = Math.max(1, Number(raw?.ratio) || 1)
  let widgets = []
  if (Array.isArray(raw?.widgets) && raw.widgets.length) {
    widgets = raw.widgets.map(normalizeWidget).filter(Boolean)
  } else if (raw?.widget) {
    const w = normalizeWidget(raw.widget)
    if (w) widgets = [w]
  }
  // widget = 首个，兼容旧编辑逻辑；widgets 支持列内上下堆叠（如待办右侧源码+文档）
  return {
    id: String(raw?.id || newId('c')),
    ratio,
    height: raw?.height ? normalizeHeight(raw.height) : null,
    widget: widgets[0] || null,
    widgets
  }
}

export function normalizeRow(raw) {
  let cols = Array.isArray(raw?.cols) ? raw.cols.map(normalizeCol) : []
  if (cols.length > MAX_COLS_PER_ROW) cols = cols.slice(0, MAX_COLS_PER_ROW)
  if (!cols.length) {
    cols = [normalizeCol({ ratio: GRID_SPAN })]
  }
  const spans = normalizeSpans(cols.map(c => c.ratio))
  cols.forEach((c, i) => {
    c.ratio = spans[i]
  })
  return {
    id: String(raw?.id || newId('r')),
    height: normalizeHeight(raw?.height, 'row'),
    cols
  }
}

export function normalizeRows(rows) {
  if (!Array.isArray(rows) || !rows.length) return []
  return rows.map(normalizeRow)
}

/** 是否为可用的 v3 配置 */
export function isV3Config(raw) {
  if (!raw || typeof raw !== 'object') return false
  if (Number(raw.version) < CONFIG_VERSION) return false
  return Array.isArray(raw.tabs?.work?.rows)
}

/**
 * 规范化 v3 配置；非法则返回 null
 */
export function normalizeBoardWidgets(raw) {
  if (!Array.isArray(raw)) return []
  return raw.map(id => String(id || '')).filter(Boolean)
}

export function normalizeV3Config(raw) {
  if (!isV3Config(raw)) return null
  const rows = normalizeRows(raw.tabs.work.rows)
  // 自定义页签变更后不再持久化 tabs.board；读入时剥离
  return {
    version: CONFIG_VERSION,
    preset: raw.preset || 'tester',
    tabs: {
      work: { rows }
    }
  }
}

/** 一行内 span → 百分比宽度（已按 24 归一） */
export function colFlexPercents(cols) {
  const list = cols || []
  const spans = normalizeSpans(list.map(c => c.ratio))
  return spans.map(s => (s / GRID_SPAN) * 100)
}

/**
 * 拖拽相邻列：按 24 格整数重分配，其它列不变。
 * @param {number[]} mins 各列最小占格（组件 minSpan）；缺省用 MIN_COL_SPAN
 */
export function resizeAdjacentSpans(spans, leftIndex, deltaUnits, mins = []) {
  const out = normalizeSpans(spans).slice()
  const i = leftIndex
  if (i < 0 || i >= out.length - 1) return out
  const pair = out[i] + out[i + 1]
  let minL = Math.max(MIN_COL_SPAN, Number(mins[i]) || MIN_COL_SPAN)
  let minR = Math.max(MIN_COL_SPAN, Number(mins[i + 1]) || MIN_COL_SPAN)
  if (minL + minR > pair) {
    const scale = pair / (minL + minR)
    minL = Math.max(MIN_COL_SPAN, Math.floor(minL * scale))
    minR = Math.max(MIN_COL_SPAN, pair - minL)
    if (minL + minR > pair) {
      minR = pair - minL
    }
  }
  let left = out[i] + Math.round(deltaUnits)
  left = Math.max(minL, Math.min(pair - minR, left))
  out[i] = left
  out[i + 1] = pair - left
  return out
}

/** 比例预设是否满足各列最小占格 */
export function spansSatisfyMins(spans, mins) {
  const s = spans || []
  const m = mins || []
  if (s.length !== m.length && m.length) {
    // 长度不一致时只校验已有 mins
  }
  for (let i = 0; i < s.length; i++) {
    const need = Math.max(MIN_COL_SPAN, Number(m[i]) || MIN_COL_SPAN)
    if (s[i] < need) return false
  }
  return true
}

/** 按列最小占格过滤比例下拉选项；当前值始终保留 */
export function filterRatioOptionsByMins(options, mins, currentLabel) {
  const list = options || []
  const cur = String(currentLabel || '')
  return list.filter(opt => {
    if (opt.value === cur) return true
    return spansSatisfyMins(opt.spans || parseSpansLabel(opt.value), mins)
  })
}

/** 从 widget type 列表生成单列多行（每组件一行通栏） */
export function rowsFromWidgetTypes(types, opts = {}) {
  const height = normalizeHeight(opts.height || { mode: 'row' })
  return (types || [])
    .filter(Boolean)
    .filter(t => t !== 'quick-create')
    .map(type =>
      normalizeRow({
        height,
        cols: [
          {
            ratio: GRID_SPAN,
            widget: { type, props: {} }
          }
        ]
      })
    )
}

/** 多列一行；entry 可为 { type, ratio, height? } 或 { stack: [{ type }], ratio, height? }（列内上下堆叠） */
export function rowWithTypes(entries, height) {
  const cols = (entries || [])
    .filter(e => e && (e.type || (Array.isArray(e.stack) && e.stack.length)))
    .slice(0, MAX_COLS_PER_ROW)
    .map(e => {
      if (Array.isArray(e.stack) && e.stack.length) {
        return {
          ratio: e.ratio || 1,
          height: e.height || null,
          widgets: e.stack
            .filter(s => s && s.type && s.type !== 'quick-create')
            .map(s => ({ type: s.type, props: s.props || {} }))
        }
      }
      if (!e.type || e.type === 'quick-create') return null
      return {
        ratio: e.ratio || 1,
        height: e.height || null,
        widget: { type: e.type, props: e.props || {} }
      }
    })
    .filter(Boolean)
  if (!cols.length) return null
  return normalizeRow({ height: height || { mode: 'row' }, cols })
}

export function setRowColCount(row, count) {
  const n = Math.min(MAX_COLS_PER_ROW, Math.max(1, Number(count) || 1))
  const next = normalizeRow(row)
  const cols = next.cols.slice()
  while (cols.length < n) {
    cols.push(normalizeCol({ ratio: MIN_COL_SPAN }))
  }
  while (cols.length > n) {
    cols.pop()
  }
  const spans = equalSpans(n)
  cols.forEach((c, i) => {
    c.ratio = spans[i]
  })
  next.cols = cols
  return next
}

export function setRowEqualRatios(row) {
  const next = normalizeRow(row)
  const spans = equalSpans(next.cols.length)
  next.cols.forEach((c, i) => {
    c.ratio = spans[i]
  })
  return next
}

/** 应用 "12:12" 这类 24 栅格标签 */
export function applyRatioString(row, ratioStr) {
  const next = normalizeRow(row)
  const parts = parseSpansLabel(ratioStr)
  if (parts.length !== next.cols.length) return next
  const spans = normalizeSpans(parts)
  next.cols.forEach((c, i) => {
    c.ratio = spans[i]
  })
  return next
}

export function colWidgets(col) {
  if (!col) return []
  if (Array.isArray(col.widgets) && col.widgets.length) {
    return col.widgets.filter(w => w && w.type)
  }
  return col.widget?.type ? [col.widget] : []
}

export function collectInstanceTypes(rows) {
  const types = []
  for (const row of rows || []) {
    for (const col of row.cols || []) {
      for (const w of colWidgets(col)) types.push(w.type)
    }
  }
  return types
}

export function findCol(rows, colId) {
  for (let ri = 0; ri < (rows || []).length; ri++) {
    const ci = rows[ri].cols.findIndex(c => c.id === colId)
    if (ci >= 0) return { rowIndex: ri, colIndex: ci, row: rows[ri], col: rows[ri].cols[ci] }
  }
  return null
}

function setColWidgetList(col, list) {
  const widgets = (list || []).map(normalizeWidget).filter(Boolean)
  col.widgets = widgets
  col.widget = widgets[0] || null
}

/** 列内是否已有指定 type（同列 type 唯一） */
export function columnHasWidgetType(col, type) {
  const t = String(type || '')
  if (!t) return false
  return colWidgets(col).some(w => w.type === t)
}

/** 按 instanceId 查找组件所在列 */
export function findWidgetInstance(rows, instanceId) {
  const id = String(instanceId || '')
  if (!id) return null
  for (let ri = 0; ri < (rows || []).length; ri++) {
    const cols = rows[ri].cols || []
    for (let ci = 0; ci < cols.length; ci++) {
      const list = colWidgets(cols[ci])
      const wi = list.findIndex(w => w.instanceId === id)
      if (wi >= 0) {
        return {
          rowIndex: ri,
          colIndex: ci,
          widgetIndex: wi,
          row: rows[ri],
          col: cols[ci],
          widget: list[wi]
        }
      }
    }
  }
  return null
}

/**
 * @deprecated 整列互换；编辑器改用 moveWidgetInstance。保留供兼容。
 */
export function swapColWidgets(rows, colIdA, colIdB) {
  const a = findCol(rows, colIdA)
  const b = findCol(rows, colIdB)
  if (!a || !b) return rows
  const next = JSON.parse(JSON.stringify(rows))
  const ca = next[a.rowIndex].cols[a.colIndex]
  const cb = next[b.rowIndex].cols[b.colIndex]
  const wa = colWidgets(ca)
  setColWidgetList(ca, colWidgets(cb))
  setColWidgetList(cb, wa)
  return next
}

/**
 * 向列追加（或按 insertIndex 插入）组件；同列已有相同 type 时原样返回 rows（引用不变）。
 * @param {number} [insertIndex] 省略则追加到末尾
 */
export function placeWidgetInCol(rows, colId, type, props = {}, insertIndex) {
  const hit = findCol(rows, colId)
  if (!hit || !type) return rows
  if (columnHasWidgetType(hit.col, type)) return rows
  const next = JSON.parse(JSON.stringify(rows))
  const col = next[hit.rowIndex].cols[hit.colIndex]
  const list = colWidgets(col).slice()
  const widget = { type, props, instanceId: newId('i') }
  const at =
    insertIndex == null || !Number.isFinite(Number(insertIndex))
      ? list.length
      : Math.max(0, Math.min(list.length, Number(insertIndex)))
  list.splice(at, 0, widget)
  setColWidgetList(col, list)
  return next
}

/** 清空整列组件 */
export function clearColWidget(rows, colId) {
  const hit = findCol(rows, colId)
  if (!hit) return rows
  const next = JSON.parse(JSON.stringify(rows))
  setColWidgetList(next[hit.rowIndex].cols[hit.colIndex], [])
  return next
}

/** 按 instanceId 删除单个组件；找不到则原样返回 */
export function removeWidgetInstance(rows, instanceId) {
  const hit = findWidgetInstance(rows, instanceId)
  if (!hit) return rows
  const next = JSON.parse(JSON.stringify(rows))
  const col = next[hit.rowIndex].cols[hit.colIndex]
  const list = colWidgets(col).slice()
  list.splice(hit.widgetIndex, 1)
  setColWidgetList(col, list)
  return next
}

/**
 * 将组件移到目标列的 toIndex（省略则末尾）；同列改序或跨列移动。
 * 跨列且目标已有相同 type 时原样返回 rows（引用不变）。
 */
export function moveWidgetInstance(rows, instanceId, toColId, toIndex) {
  const from = findWidgetInstance(rows, instanceId)
  const to = findCol(rows, toColId)
  if (!from || !to) return rows
  const sameCol = from.col.id === to.col.id
  if (!sameCol && columnHasWidgetType(to.col, from.widget.type)) return rows

  const next = JSON.parse(JSON.stringify(rows))
  const srcCol = next[from.rowIndex].cols[from.colIndex]
  const dstHit = findCol(next, toColId)
  if (!dstHit) return rows
  const dstCol = next[dstHit.rowIndex].cols[dstHit.colIndex]

  const srcList = colWidgets(srcCol).slice()
  const [widget] = srcList.splice(from.widgetIndex, 1)
  if (!widget) return rows

  if (sameCol) {
    // toIndex = 落点组件在「移除前」的下标，作为移动后的最终下标（上面拖到下面即可换位）
    // 不要在 from < to 时再 -1：那会把「拖到下一个」变成空操作
    let at =
      toIndex == null || !Number.isFinite(Number(toIndex))
        ? srcList.length
        : Number(toIndex)
    at = Math.max(0, Math.min(srcList.length, at))
    srcList.splice(at, 0, widget)
    setColWidgetList(srcCol, srcList)
    return next
  }

  setColWidgetList(srcCol, srcList)
  const dstList = colWidgets(dstCol).slice()
  const at =
    toIndex == null || !Number.isFinite(Number(toIndex))
      ? dstList.length
      : Math.max(0, Math.min(dstList.length, Number(toIndex)))
  dstList.splice(at, 0, widget)
  setColWidgetList(dstCol, dstList)
  return next
}
