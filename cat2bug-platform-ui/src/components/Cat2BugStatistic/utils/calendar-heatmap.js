/** 日历热力图：列=周，行=周日~周六（0=日） */
export const WEEKDAY_LABELS = ['日', '一', '二', '三', '四', '五', '六']

function parseDateStr(dateStr) {
  const [y, m, d] = dateStr.split('-').map(Number)
  return new Date(y, m - 1, d)
}

function formatDateStr(dt) {
  const y = dt.getFullYear()
  const m = String(dt.getMonth() + 1).padStart(2, '0')
  const d = String(dt.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

/**
 * 构建完整周×日网格（含首尾周留白格，pad=true）
 * @returns {{ slots, weekCount, max, allZero, dateLookup }}
 */
export function buildCalendarHeatmapData(dayList) {
  const dateLookup = new Map()
  if (!dayList || !dayList.length) {
    return { slots: [], weekCount: 0, max: 1, allZero: true, dateLookup, cells: [], xWeeks: [] }
  }

  const countMap = {}
  const sorted = [...dayList].sort((a, b) => String(a.date).localeCompare(String(b.date)))
  sorted.forEach((d) => {
    countMap[d.date] = d.count || 0
  })

  const start = parseDateStr(sorted[0].date)
  const end = parseDateStr(sorted[sorted.length - 1].date)
  let col = 0
  const cur = new Date(start)

  while (cur <= end) {
    const dow = cur.getDay()
    if (cur > start && dow === 0) {
      col += 1
    }
    const dateStr = formatDateStr(cur)
    dateLookup.set(`${col},${dow}`, dateStr)
    cur.setDate(cur.getDate() + 1)
  }

  const weekCount = col + 1
  const slots = []
  const cells = []

  for (let c = 0; c < weekCount; c++) {
    for (let d = 0; d < 7; d++) {
      const key = `${c},${d}`
      const date = dateLookup.get(key) || null
      const pad = !date
      const count = date ? (countMap[date] || 0) : 0
      slots.push({ col: c, dow: d, date, count: pad ? 0 : count, pad })
      if (!pad) {
        cells.push([c, d, count])
      }
    }
  }

  const maxCount = cells.reduce((m, c) => Math.max(m, c[2] || 0), 0)
  const max = maxCount > 0 ? maxCount : 1
  const xWeeks = []
  for (let i = 0; i < weekCount; i++) {
    xWeeks.push(String(i + 1))
  }

  return {
    slots,
    weekCount,
    max,
    maxCount,
    allZero: maxCount === 0,
    dateLookup,
    cells,
    xWeeks
  }
}

/** 按参与次数在色阶上取色 */
export function heatColorForCount(count, max, theme) {
  if (!count || count <= 0) {
    return theme.emptyCell
  }
  const t = Math.min(1, count / max)
  const palette = theme.heatActive || theme.heat.slice(1)
  const idx = Math.min(palette.length - 1, Math.floor(t * palette.length))
  return palette[idx]
}
