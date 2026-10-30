/** 默认工作周：周一～五（周一=1 … 周日=7） */
export function defaultWorkWeek() {
  return [1, 2, 3, 4, 5]
}

export function createDefaultWorkCalendar() {
  return {
    projectId: null,
    workWeek: defaultWorkWeek(),
    exceptions: {},
    editable: false
  }
}

/**
 * 规范化后端日历；失败时返回默认一～五。
 * @param {object|null|undefined} raw
 */
export function normalizeWorkCalendar(raw) {
  const base = createDefaultWorkCalendar()
  if (!raw || typeof raw !== 'object') return base
  const week = Array.isArray(raw.workWeek)
    ? raw.workWeek.map((n) => Number(n)).filter((n) => n >= 1 && n <= 7)
    : []
  const exceptions = {}
  const src = raw.exceptions && typeof raw.exceptions === 'object' ? raw.exceptions : {}
  Object.keys(src).forEach((k) => {
    const v = String(src[k] || '').toLowerCase()
    if (v === 'holiday' || v === 'workday') exceptions[k] = v
  })
  return {
    projectId: raw.projectId != null ? raw.projectId : null,
    workWeek: week.length ? week : defaultWorkWeek(),
    exceptions,
    editable: !!raw.editable,
    updateBy: raw.updateBy,
    updateTime: raw.updateTime
  }
}

/** 全自然日日历（忽略工作日历时用） */
export function naturalDayCalendar() {
  return {
    workWeek: [1, 2, 3, 4, 5, 6, 7],
    exceptions: {}
  }
}

export function formatDateKey(input) {
  const d = input instanceof Date ? input : new Date(input)
  if (Number.isNaN(d.getTime())) return null
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** JS getDay: 0=周日 → ISO 周一=1…周日=7 */
export function isoWeekday(date) {
  const d = date instanceof Date ? date : new Date(date)
  const js = d.getDay()
  return js === 0 ? 7 : js
}
