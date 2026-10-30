/** 缺陷甘特：常量与投影（处理人镜像 / 交付物树序缩进） */

export const GANTT_PAGE_SIZE = 200
export const GANTT_DEFAULT_DURATION_DAYS = 1

export const GANTT_GROUP_BY = Object.freeze({
  HANDLER: 'handler',
  MODULE: 'module'
})

export const GANTT_SCALE = Object.freeze({
  DAY: 'day',
  WEEK: 'week',
  MONTH: 'month'
})

export const GANTT_CACHE = Object.freeze({
  LEFT_WIDTH: 'defect.gantt.leftWidth',
  GROUP_BY: 'defect.gantt.groupBy',
  SCALE: 'defect.gantt.scale',
  /** 分组内缺陷行序 { [groupKey]: defectId[] } */
  ROW_ORDER: 'defect.gantt.rowOrder',
  /** 已折叠分组 key 列表 */
  COLLAPSED: 'defect.gantt.collapsedGroups',
  /** 未排期池高度 */
  UNSCHEDULED_HEIGHT: 'defect.gantt.unscheduledHeight',
  /** 未排期池是否折叠 */
  UNSCHEDULED_COLLAPSED: 'defect.gantt.unscheduledCollapsed',
  /** 图层开关 { deps, critical, load, baseline } */
  LAYERS: 'defect.gantt.layers'
})

export const GANTT_LEFT_WIDTH_DEFAULT = 280
export const GANTT_LEFT_WIDTH_MIN = 180
export const GANTT_LEFT_WIDTH_MAX = 480

export const GANTT_UNSCHEDULED_HEIGHT_DEFAULT = 140
export const GANTT_UNSCHEDULED_HEIGHT_MIN = 72
export const GANTT_UNSCHEDULED_HEIGHT_MAX = 420

export const GANTT_ROW_HEIGHT = 36
/** 双层时间头：上层月份/年 + 下层日/周/月 */
export const GANTT_HEADER_HEIGHT = 60
export const GANTT_HEADER_MAJOR_HEIGHT = 24
/** 下层刻度含「日+周几」两行，略增高 */
export const GANTT_HEADER_MINOR_HEIGHT = 36
export const GANTT_GROUP_HEADER_HEIGHT = 40

/** 按处理人分组时，组底资源负荷轨高度（参考图：导轨 + 矮蓝条/高粉块） */
export const GANTT_LOAD_TRACK_HEIGHT = 26

/** 日/周/月列宽（px） */
export const GANTT_COL_WIDTH = Object.freeze({
  day: 36,
  week: 72,
  month: 96
})

const STATE_CLASS = Object.freeze({
  PROCESSING: 'processing',
  AUDIT: 'audit',
  RESOLVED: 'resolved',
  REJECTED: 'rejected',
  CLOSED: 'closed'
})

const DAY_MS = 24 * 60 * 60 * 1000

export function startOfDay(input) {
  const d = input instanceof Date ? new Date(input.getTime()) : new Date(input)
  if (Number.isNaN(d.getTime())) return null
  d.setHours(0, 0, 0, 0)
  return d
}

export function endOfDay(input) {
  const d = startOfDay(input)
  if (!d) return null
  d.setHours(23, 59, 59, 0)
  return d
}

export function addDays(input, days) {
  const d = startOfDay(input)
  if (!d) return null
  d.setDate(d.getDate() + days)
  return d
}

export function parsePlanDate(value) {
  if (value == null || value === '') return null
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? null : d
}

/** 有起止计划时间即可画条 */
export function isScheduled(defect) {
  if (!defect) return false
  return !!(parsePlanDate(defect.planStartTime) && parsePlanDate(defect.planEndTime))
}

export function defectStateClass(defect) {
  const name = defect && defect.defectStateName
  return STATE_CLASS[name] || 'unknown'
}

/**
 * 解析处理人列表：优先 handleByList，回退 handleBy id 数组。
 * 保留 avatar 等字段供 Cat2BugAvatar 使用。
 * @returns {{ userId: number, nickName: string, avatar?: string, avatarUrl?: string, userName?: string }[]}
 */
export function resolveHandlers(defect) {
  if (!defect) return []
  const list = defect.handleByList
  if (Array.isArray(list) && list.length) {
    return list
      .map((m) => {
        if (!m) return null
        const userId = Number(m.userId != null ? m.userId : m.id)
        if (!Number.isFinite(userId)) return null
        const nickName = m.nickName || m.userName || String(userId)
        return {
          userId,
          nickName,
          userName: m.userName || nickName,
          avatar: m.avatar,
          avatarUrl: m.avatarUrl
        }
      })
      .filter(Boolean)
  }
  const ids = defect.handleBy
  if (!Array.isArray(ids) || !ids.length) return []
  return ids
    .map((id) => {
      const userId = Number(id)
      if (!Number.isFinite(userId)) return null
      return { userId, nickName: String(userId), userName: String(userId) }
    })
    .filter(Boolean)
}

export function formatPlanDateTime(date) {
  const d = date instanceof Date ? date : new Date(date)
  if (Number.isNaN(d.getTime())) return ''
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const i = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${i}:${s}`
}

/** 落点日为开始，默认时长 1 个自然日（当日 00:00 ~ 23:59:59） */
export function defaultPlanRangeFromDropDay(dayDate) {
  const start = startOfDay(dayDate)
  if (!start) return null
  return {
    planStartTime: formatPlanDateTime(start),
    planEndTime: formatPlanDateTime(endOfDay(start))
  }
}

export function shiftPlanRange(startValue, endValue, deltaDays) {
  const start = parsePlanDate(startValue)
  const end = parsePlanDate(endValue)
  if (!start || !end) return null
  const ns = new Date(start.getTime() + deltaDays * DAY_MS)
  const ne = new Date(end.getTime() + deltaDays * DAY_MS)
  return {
    planStartTime: formatPlanDateTime(ns),
    planEndTime: formatPlanDateTime(ne)
  }
}

/** 拉边改期；保证至少 1 个自然日（startOfDay(start) <= startOfDay(end)） */
export function resizePlanRange(startValue, endValue, edge, dayDate) {
  const start = parsePlanDate(startValue)
  const end = parsePlanDate(endValue)
  const day = startOfDay(dayDate)
  if (!start || !end || !day) return null
  if (edge === 'start') {
    let ns = new Date(day)
    ns.setHours(start.getHours(), start.getMinutes(), start.getSeconds(), 0)
    const endDay = startOfDay(end)
    if (ns.getTime() > endDay.getTime()) {
      ns = new Date(endDay)
      ns.setHours(start.getHours(), start.getMinutes(), start.getSeconds(), 0)
    }
    return {
      planStartTime: formatPlanDateTime(ns),
      planEndTime: formatPlanDateTime(end)
    }
  }
  if (edge === 'end') {
    let ne = endOfDay(day)
    const startDay = startOfDay(start)
    if (ne.getTime() < startDay.getTime()) {
      ne = endOfDay(startDay)
    }
    return {
      planStartTime: formatPlanDateTime(start),
      planEndTime: formatPlanDateTime(ne)
    }
  }
  return null
}

/**
 * @typedef {object} GanttRow
 * @property {string} rowKey
 * @property {object} defect
 * @property {boolean} scheduled
 * @property {boolean} mirrored
 * @property {{ userId: number, nickName: string }[]} otherHandlers
 * @property {number|null} groupHandlerId
 * @property {number|string|null} groupModuleId
 */

/**
 * @typedef {object} GanttGroup
 * @property {string} key
 * @property {string} label 展示名（交付物为路径末段）
 * @property {'handler'|'module'} type
 * @property {number|null} handlerId
 * @property {object|null} handlerMember 处理人完整信息（含头像）
 * @property {number|string|null} moduleId
 * @property {string} [modulePath] 交付物全路径（`/` 分隔）
 * @property {number} [depth] 交付物树深度（根=0）
 * @property {boolean} [structural] 仅作层级占位、自身无已排期缺陷的祖先组
 * @property {GanttRow[]} rows
 */

/**
 * 拆分交付物全路径。
 * @param {string|null|undefined} path
 * @returns {string[]}
 */
export function splitModulePath(path) {
  if (path == null || path === '') return []
  return String(path)
    .split('/')
    .map((s) => s.trim())
    .filter(Boolean)
}

/**
 * 已排期投影为分组行；未排期单独返回（不进分组泳道）。
 * @param {object[]} defects
 * @param {'handler'|'module'} groupBy
 * @param {Record<string|number, string>} [modulePathById] moduleId → 全路径
 * @returns {{ groups: GanttGroup[], unscheduled: object[] }}
 */
export function projectGanttRows(defects, groupBy, modulePathById) {
  const list = Array.isArray(defects) ? defects : []
  const scheduled = []
  const unscheduled = []
  for (let i = 0; i < list.length; i++) {
    const d = list[i]
    if (!d || d.defectId == null) continue
    if (isScheduled(d)) scheduled.push(d)
    else unscheduled.push(d)
  }

  if (groupBy === GANTT_GROUP_BY.MODULE) {
    return { groups: projectByModule(scheduled, modulePathById), unscheduled }
  }
  return { groups: projectByHandler(scheduled), unscheduled }
}

function projectByHandler(defects) {
  /** @type {Map<string, GanttGroup>} */
  const map = new Map()
  for (let i = 0; i < defects.length; i++) {
    const defect = defects[i]
    const handlers = resolveHandlers(defect)
    if (!handlers.length) continue
    const mirrored = handlers.length > 1
    for (let h = 0; h < handlers.length; h++) {
      const handler = handlers[h]
      const key = `h:${handler.userId}`
      let group = map.get(key)
      if (!group) {
        group = {
          key,
          label: handler.nickName,
          type: 'handler',
          handlerId: handler.userId,
          handlerMember: { ...handler },
          moduleId: null,
          rows: []
        }
        map.set(key, group)
      } else if (handler.avatar || handler.avatarUrl) {
        /* 后续缺陷若带头像，补全组头像 */
        const m = group.handlerMember || {}
        if (!m.avatar && !m.avatarUrl) {
          group.handlerMember = { ...m, ...handler }
          group.label = handler.nickName || group.label
        }
      }
      const otherHandlers = handlers.filter((x) => x.userId !== handler.userId)
      group.rows.push({
        rowKey: `${defect.defectId}:${handler.userId}`,
        defect,
        scheduled: true,
        mirrored,
        otherHandlers,
        groupHandlerId: handler.userId,
        groupModuleId: null
      })
    }
  }
  return sortGroups(Array.from(map.values()))
}

/**
 * @param {object[]} defects
 * @param {Record<string|number, string>} [modulePathById]
 */
function projectByModule(defects, modulePathById) {
  const pathById = modulePathById && typeof modulePathById === 'object' ? modulePathById : {}
  /** @type {Record<string, string|number>} */
  const idByPath = {}
  Object.keys(pathById).forEach((id) => {
    const p = pathById[id]
    if (p == null || p === '') return
    idByPath[String(p)] = id
  })

  /** @type {Map<string, GanttGroup>} */
  const map = new Map()
  for (let i = 0; i < defects.length; i++) {
    const defect = defects[i]
    const rawModuleId = defect.moduleId
    const moduleId =
      rawModuleId == null || rawModuleId === '' || rawModuleId === '__none__'
        ? '__none__'
        : String(rawModuleId)
    const key = `m:${moduleId}`
    let group = map.get(key)
    if (!group) {
      const resolvedPath =
        moduleId === '__none__'
          ? ''
          : String(
            (pathById[moduleId] != null && pathById[moduleId] !== ''
              ? pathById[moduleId]
              : null) ||
              defect.modulePath ||
              defect.moduleName ||
              moduleId
          ).trim()
      const parts = splitModulePath(resolvedPath)
      const leaf =
        moduleId === '__none__'
          ? ''
          : (parts.length ? parts[parts.length - 1] : (defect.moduleName || resolvedPath || moduleId))
      group = {
        key,
        label: leaf,
        type: 'module',
        handlerId: null,
        moduleId: moduleId === '__none__' ? null : (Number.isFinite(Number(rawModuleId)) ? Number(rawModuleId) : rawModuleId),
        modulePath: moduleId === '__none__' ? '' : resolvedPath,
        depth: moduleId === '__none__' ? 0 : Math.max(0, parts.length - 1),
        structural: false,
        rows: []
      }
      map.set(key, group)
    }
    group.structural = false
    group.rows.push({
      rowKey: `${defect.defectId}:m`,
      defect,
      scheduled: true,
      mirrored: false,
      otherHandlers: [],
      groupHandlerId: null,
      groupModuleId: group.moduleId
    })
  }

  /* 为已出现的叶子补齐祖先占位组，使左侧呈现交付物树层级 */
  const present = Array.from(map.values())
  for (let i = 0; i < present.length; i++) {
    const g = present[i]
    if (!g || g.key === 'm:__none__' || !g.modulePath) continue
    const parts = splitModulePath(g.modulePath)
    for (let d = 1; d < parts.length; d++) {
      const ancestorPath = parts.slice(0, d).join('/')
      const rawId = idByPath[ancestorPath]
      if (rawId == null || rawId === '') continue
      const ancestorId = String(rawId)
      const key = `m:${ancestorId}`
      if (map.has(key)) continue
      map.set(key, {
        key,
        label: parts[d - 1],
        type: 'module',
        handlerId: null,
        moduleId: Number.isFinite(Number(rawId)) ? Number(rawId) : rawId,
        modulePath: ancestorPath,
        depth: d - 1,
        structural: true,
        rows: []
      })
    }
  }

  return sortModuleGroups(Array.from(map.values()))
}

function sortGroups(groups) {
  groups.sort((a, b) => String(a.label || '').localeCompare(String(b.label || ''), 'zh'))
  return groups
}

/** 无交付物置顶，其余按全路径树序 */
function sortModuleGroups(groups) {
  groups.sort((a, b) => {
    const aNone = a.key === 'm:__none__'
    const bNone = b.key === 'm:__none__'
    if (aNone !== bNone) return aNone ? -1 : 1
    const ap = String(a.modulePath || a.label || '')
    const bp = String(b.modulePath || b.label || '')
    const byPath = ap.localeCompare(bp, 'zh')
    if (byPath !== 0) return byPath
    return String(a.label || '').localeCompare(String(b.label || ''), 'zh')
  })
  for (let i = 0; i < groups.length; i++) {
    const g = groups[i]
    if (!g) continue
    if (g.key === 'm:__none__') {
      g.depth = 0
      g.modulePath = g.modulePath || ''
      continue
    }
    const parts = splitModulePath(g.modulePath)
    g.depth = parts.length ? parts.length - 1 : 0
  }
  return groups
}

/**
 * 上层大刻度：日/周尺度按自然月合并；月尺度按年合并。
 * @returns {{ key: string, label: string, unitCount: number, start: Date }[]}
 */
export function buildAxisMajorBands(units, scale) {
  const list = Array.isArray(units) ? units : []
  if (!list.length) return []
  const bands = []
  let cur = null
  for (let i = 0; i < list.length; i++) {
    const u = list[i]
    const d = u.start instanceof Date ? u.start : new Date(u.start)
    let bandKey
    let label
    if (scale === GANTT_SCALE.MONTH) {
      bandKey = `y:${d.getFullYear()}`
      label = `${d.getFullYear()}`
    } else {
      bandKey = `ym:${d.getFullYear()}-${d.getMonth()}`
      label = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
    }
    if (!cur || cur.key !== bandKey) {
      cur = { key: bandKey, label, unitCount: 1, start: d }
      bands.push(cur)
    } else {
      cur.unitCount += 1
    }
  }
  return bands
}

export function buildAxisUnits(rangeStart, rangeEnd, scale) {
  const start = startOfDay(rangeStart)
  const end = startOfDay(rangeEnd)
  if (!start || !end || end.getTime() < start.getTime()) return []
  const units = []
  if (scale === GANTT_SCALE.WEEK) {
    const cur = startOfWeek(start)
    const last = startOfWeek(end)
    while (cur.getTime() <= last.getTime()) {
      units.push({ start: new Date(cur), end: addDays(cur, 7), key: `w:${cur.toISOString()}` })
      cur.setDate(cur.getDate() + 7)
    }
    return units
  }
  if (scale === GANTT_SCALE.MONTH) {
    const cur = startOfMonth(start)
    const last = startOfMonth(end)
    while (cur.getTime() <= last.getTime()) {
      const next = new Date(cur.getFullYear(), cur.getMonth() + 1, 1)
      units.push({ start: new Date(cur), end: next, key: `m:${cur.getFullYear()}-${cur.getMonth()}` })
      cur.setFullYear(next.getFullYear(), next.getMonth(), 1)
    }
    return units
  }
  const cur = new Date(start)
  while (cur.getTime() <= end.getTime()) {
    const day = new Date(cur)
    units.push({
      start: day,
      end: addDays(day, 1),
      key: `d:${day.toISOString()}`,
      weekend: day.getDay() === 0 || day.getDay() === 6
    })
    cur.setDate(cur.getDate() + 1)
  }
  return units
}

export function startOfWeek(input) {
  const d = startOfDay(input)
  if (!d) return null
  const day = d.getDay()
  const diff = day === 0 ? -6 : 1 - day
  d.setDate(d.getDate() + diff)
  return d
}

/** ISO 周序号（周一为一周之始），用于周尺度刻度 */
export function isoWeekNumber(input) {
  const d = startOfDay(input)
  if (!d) return null
  const tmp = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()))
  const dayNum = tmp.getUTCDay() || 7
  tmp.setUTCDate(tmp.getUTCDate() + 4 - dayNum)
  const yearStart = new Date(Date.UTC(tmp.getUTCFullYear(), 0, 1))
  return Math.ceil((((tmp - yearStart) / 86400000) + 1) / 7)
}

export function startOfMonth(input) {
  const d = startOfDay(input)
  if (!d) return null
  d.setDate(1)
  return d
}

/**
 * 根据已排期缺陷计算可视轴范围（含前后缓冲）。
 */
export function computeAxisRange(defects, scale, now = new Date()) {
  const today = startOfDay(now)
  let min = today
  let max = addDays(today, scale === GANTT_SCALE.MONTH ? 90 : scale === GANTT_SCALE.WEEK ? 56 : 28)
  const list = Array.isArray(defects) ? defects : []
  for (let i = 0; i < list.length; i++) {
    const s = parsePlanDate(list[i].planStartTime)
    const e = parsePlanDate(list[i].planEndTime)
    if (s && (!min || s < min)) min = startOfDay(s)
    if (e && (!max || e > max)) max = startOfDay(e)
  }
  const padBefore = scale === GANTT_SCALE.MONTH ? 15 : scale === GANTT_SCALE.WEEK ? 7 : 3
  const padAfter = scale === GANTT_SCALE.MONTH ? 45 : scale === GANTT_SCALE.WEEK ? 21 : 14
  return {
    start: addDays(min, -padBefore),
    end: addDays(max, padAfter)
  }
}

export function dateToOffsetPx(date, rangeStart, scale) {
  const d = parsePlanDate(date)
  const start = startOfDay(rangeStart)
  if (!d || !start) return 0
  const col = GANTT_COL_WIDTH[scale] || GANTT_COL_WIDTH.day
  if (scale === GANTT_SCALE.WEEK) {
    const days = (startOfDay(d).getTime() - start.getTime()) / DAY_MS
    return (days / 7) * col
  }
  if (scale === GANTT_SCALE.MONTH) {
    const months =
      (d.getFullYear() - start.getFullYear()) * 12 +
      (d.getMonth() - start.getMonth()) +
      (d.getDate() - 1) / 31
    return months * col
  }
  const days = (startOfDay(d).getTime() - start.getTime()) / DAY_MS
  return days * col
}

export function offsetPxToDate(offsetPx, rangeStart, scale) {
  const start = startOfDay(rangeStart)
  if (!start) return null
  const col = GANTT_COL_WIDTH[scale] || GANTT_COL_WIDTH.day
  if (scale === GANTT_SCALE.WEEK) {
    const weeks = offsetPx / col
    return addDays(start, Math.floor(weeks * 7))
  }
  if (scale === GANTT_SCALE.MONTH) {
    const months = offsetPx / col
    const whole = Math.floor(months)
    const d = new Date(start.getFullYear(), start.getMonth() + whole, 1)
    const frac = months - whole
    d.setDate(1 + Math.floor(frac * 28))
    return startOfDay(d)
  }
  const days = Math.floor(offsetPx / col)
  return addDays(start, days)
}

export function barGeometry(defect, rangeStart, scale) {
  const s = parsePlanDate(defect.planStartTime)
  const e = parsePlanDate(defect.planEndTime)
  if (!s || !e) return null
  const x = dateToOffsetPx(s, rangeStart, scale)
  const x2 = dateToOffsetPx(addDays(startOfDay(e), 1), rangeStart, scale)
  const raw = x2 - x
  /*
   * 日尺度：1 天 ≈ 1 列，保底一列便于点选。
   * 周/月：按真实日期比例（1 天 ≈ 列宽/7 或 /31），勿再保底整列，否则短任务会占半格/整格。
   */
  const minPx = scale === GANTT_SCALE.DAY ? GANTT_COL_WIDTH.day || 36 : 4
  const width = Math.max(minPx, raw)
  return { x, width }
}

/**
 * 计划跨度天数（与条块一致：起止自然日含首尾）。
 * 若起止带时分秒，按毫秒差换算，可出现小数。
 * @returns {number|null}
 */
export function planDurationDays(defect) {
  const s = parsePlanDate(defect && defect.planStartTime)
  const e = parsePlanDate(defect && defect.planEndTime)
  if (!s || !e) return null
  const a = startOfDay(s)
  const b = startOfDay(e)
  if (!a || !b || b.getTime() < a.getTime()) return null
  const startMs = s.getTime()
  const endMs = e.getTime()
  const hasTime =
    s.getHours() || s.getMinutes() || s.getSeconds() || s.getMilliseconds() ||
    e.getHours() || e.getMinutes() || e.getSeconds() || e.getMilliseconds()
  if (hasTime) {
    const raw = (endMs - startMs) / DAY_MS
    return raw > 0 ? raw : null
  }
  return (b.getTime() - a.getTime()) / DAY_MS + 1
}

/** 最多保留 1 位小数；整数不带小数点 */
export function formatDurationDays(days) {
  if (days == null || !Number.isFinite(days) || days < 0) return ''
  const r = Math.round(days * 10) / 10
  if (r === 0) return '0'
  return Number.isInteger(r) ? String(r) : r.toFixed(1)
}

/**
 * 当前结果集上的工期关键路径（与手动依赖解耦）。
 * 用计划起止推断 FS：前置结束日的次日 ≤ 后继开始日，则可衔接；
 * 只高亮一条代表性最长链（同日并列只留链上一个点，不把零时差兄弟全标红）。
 * @param {object[]} defects 已排期缺陷
 * @returns {{ ids: Set<number>, ordered: number[] }}
 */
export function computeCriticalPath(defects /* , links */) {
  const empty = { ids: new Set(), ordered: [] }
  const nodes = []
  const list = Array.isArray(defects) ? defects : []
  for (let i = 0; i < list.length; i++) {
    const d = list[i]
    if (!d || d.defectId == null) continue
    const id = Number(d.defectId)
    if (!Number.isFinite(id)) continue
    const s = startOfDay(parsePlanDate(d.planStartTime))
    const e = startOfDay(parsePlanDate(d.planEndTime))
    if (!s || !e || e.getTime() < s.getTime()) continue
    const days = planDurationDays(d)
    nodes.push({
      id,
      startMs: s.getTime(),
      endMs: e.getTime(),
      duration: days != null && Number.isFinite(days) && days > 0 ? days : 1
    })
  }
  if (!nodes.length) return empty

  /* 同 id 多行时保留一条（取较长工期） */
  const byId = new Map()
  for (let i = 0; i < nodes.length; i++) {
    const n = nodes[i]
    const prevNode = byId.get(n.id)
    if (!prevNode || n.duration > prevNode.duration) byId.set(n.id, n)
  }
  const uniq = Array.from(byId.values())
  uniq.sort((a, b) => a.startMs - b.startMs || a.endMs - b.endMs || a.id - b.id)

  const outs = uniq.map(() => [])
  const indeg = uniq.map(() => 0)
  for (let i = 0; i < uniq.length; i++) {
    const a = uniq[i]
    const earliestNext = addDays(new Date(a.endMs), 1)
    if (!earliestNext) continue
    const nextMs = earliestNext.getTime()
    for (let j = 0; j < uniq.length; j++) {
      if (i === j) continue
      const b = uniq[j]
      if (b.startMs >= nextMs) {
        outs[i].push(j)
        indeg[j] += 1
      }
    }
  }

  const n = uniq.length
  const forward = new Array(n).fill(0)
  const prev = new Array(n).fill(-1)
  const q = []
  for (let i = 0; i < n; i++) {
    forward[i] = uniq[i].duration
    if (indeg[i] === 0) q.push(i)
  }
  const indegWork = indeg.slice()
  let seen = 0
  while (q.length) {
    const u = q.shift()
    seen += 1
    const uOut = outs[u]
    for (let k = 0; k < uOut.length; k++) {
      const v = uOut[k]
      const cand = forward[u] + uniq[v].duration
      /* 平局取较小 defectId，保证同日并列只留一个前驱 */
      if (
        cand > forward[v] ||
        (cand === forward[v] && (prev[v] < 0 || uniq[u].id < uniq[prev[v]].id))
      ) {
        forward[v] = cand
        prev[v] = u
      }
      indegWork[v] -= 1
      if (indegWork[v] === 0) q.push(v)
    }
  }
  /* 环或异常：只取最晚结束日上 id 最小的一条 */
  if (seen < n) {
    let best = null
    for (let i = 0; i < n; i++) {
      const cur = uniq[i]
      if (
        !best ||
        cur.endMs > best.endMs ||
        (cur.endMs === best.endMs && cur.id < best.id)
      ) {
        best = cur
      }
    }
    if (!best) return empty
    return { ids: new Set([best.id]), ordered: [best.id] }
  }

  /* 代表性有序链：从最长 forward 的汇点回溯；平局取较晚结束、再取较小 id */
  let sink = 0
  for (let i = 1; i < n; i++) {
    if (forward[i] > forward[sink]) {
      sink = i
    } else if (forward[i] === forward[sink]) {
      if (
        uniq[i].endMs > uniq[sink].endMs ||
        (uniq[i].endMs === uniq[sink].endMs && uniq[i].id < uniq[sink].id)
      ) {
        sink = i
      }
    }
  }
  const orderedRev = []
  let cur = sink
  const guard = new Set()
  while (cur >= 0 && !guard.has(cur)) {
    guard.add(cur)
    orderedRev.push(uniq[cur].id)
    cur = prev[cur]
  }
  orderedRev.reverse()

  /* 只高亮这一条链，避免同日零时差兄弟全部标红 */
  return { ids: new Set(orderedRev), ordered: orderedRev }
}

/**
 * @returns {Set<number>}
 */
export function computeCriticalPathIds(defects, links) {
  return computeCriticalPath(defects, links).ids
}

/**
 * 将处理人负荷画成时间轴上的连续段（参考图：正常浅蓝矮条 / 过载粉红高块）。
 * 用扫线按真实条块起止切分，使粉块与上方重叠任务对齐。
 * @param {number} handlerId
 * @param {object[]} defects
 * @param {Date|string|number} rangeStart 轴起点（与 barGeometry 一致）
 * @param {string} scale
 * @param {number} [threshold=1] 超过则视为过载
 * @param {number} [axisWidthPx] 裁剪到可视轴宽
 * @returns {{ left:number, width:number, level:1|2, count:number }[]}
 */
export function buildHandlerLoadTrackSegments(
  handlerId,
  defects,
  rangeStart,
  scale,
  threshold = 1,
  axisWidthPx = Infinity
) {
  const uid = Number(handlerId)
  if (!Number.isFinite(uid) || !rangeStart) return []

  /** @type {{ t:number, d:number }[]} */
  const events = []
  const list = Array.isArray(defects) ? defects : []
  for (let i = 0; i < list.length; i++) {
    const d = list[i]
    if (!isScheduled(d)) continue
    const handlers = resolveHandlers(d)
    if (!handlers.some((h) => Number(h.userId) === uid)) continue
    const s = startOfDay(parsePlanDate(d.planStartTime))
    const e = startOfDay(parsePlanDate(d.planEndTime))
    if (!s || !e) continue
    /* 计划含首尾：与条块一致，覆盖到 end 日结束 */
    events.push({ t: s.getTime(), d: 1 })
    events.push({ t: addDays(e, 1).getTime(), d: -1 })
  }
  if (!events.length) return []

  events.sort((a, b) => (a.t !== b.t ? a.t - b.t : a.d - b.d))

  const raw = []
  let count = 0
  let segStart = null
  let segCount = 0
  for (let i = 0; i < events.length; i++) {
    const ev = events[i]
    if (segStart != null && ev.t > segStart && segCount > 0) {
      raw.push({ startMs: segStart, endMs: ev.t, count: segCount })
    }
    count += ev.d
    segStart = ev.t
    segCount = count
  }

  const maxW = Number.isFinite(axisWidthPx) && axisWidthPx > 0 ? axisWidthPx : Infinity
  const segs = []
  for (let i = 0; i < raw.length; i++) {
    const r = raw[i]
    const level = r.count > threshold ? 2 : 1
    let left = dateToOffsetPx(new Date(r.startMs), rangeStart, scale)
    let right = dateToOffsetPx(new Date(r.endMs), rangeStart, scale)
    if (right <= 0 || left >= maxW) continue
    left = Math.max(0, left)
    right = Math.min(maxW, right)
    const width = right - left
    if (width < 1) continue
    const prev = segs[segs.length - 1]
    if (
      prev &&
      prev.level === level &&
      Math.abs(prev.left + prev.width - left) < 0.5
    ) {
      prev.width = right - prev.left
      prev.count = Math.max(prev.count, r.count)
    } else {
      segs.push({ left, width, level, count: r.count })
    }
  }
  return segs
}

/**
 * 处理人 × 自然日占用条数（可见窗内）。
 * @returns {{ byHandler: Map<number,{ nickName:string, days:Map<string,number> }>, overloadKeys: Set<string> }}
 */
export function computeHandlerDayLoad(defects, rangeStart, rangeEnd, threshold = 1) {
  const byHandler = new Map()
  const overloadKeys = new Set()
  const winS = startOfDay(rangeStart)
  const winE = startOfDay(rangeEnd)
  if (!winS || !winE) return { byHandler, overloadKeys }
  const list = Array.isArray(defects) ? defects : []
  for (let i = 0; i < list.length; i++) {
    const defect = list[i]
    if (!isScheduled(defect)) continue
    const s = startOfDay(parsePlanDate(defect.planStartTime))
    const e = startOfDay(parsePlanDate(defect.planEndTime))
    if (!s || !e) continue
    const handlers = resolveHandlers(defect)
    if (!handlers.length) continue
    const from = s.getTime() > winS.getTime() ? s : winS
    const to = e.getTime() < winE.getTime() ? e : winE
    if (to.getTime() < from.getTime()) continue
    for (let h = 0; h < handlers.length; h++) {
      const handler = handlers[h]
      let bucket = byHandler.get(handler.userId)
      if (!bucket) {
        bucket = { nickName: handler.nickName, days: new Map() }
        byHandler.set(handler.userId, bucket)
      }
      const cur = new Date(from.getTime())
      while (cur.getTime() <= to.getTime()) {
        const key = `${cur.getFullYear()}-${String(cur.getMonth() + 1).padStart(2, '0')}-${String(cur.getDate()).padStart(2, '0')}`
        const n = (bucket.days.get(key) || 0) + 1
        bucket.days.set(key, n)
        if (n > threshold) overloadKeys.add(`${handler.userId}:${key}`)
        cur.setDate(cur.getDate() + 1)
      }
    }
  }
  return { byHandler, overloadKeys }
}

/**
 * FS 冲突：后继计划开始早于前置计划结束日的次日（按自然日）。
 * @returns {{ fromDefectId:number, toDefectId:number }[]}
 */
export function findFsScheduleConflicts(defects, links) {
  const byId = new Map()
  ;(Array.isArray(defects) ? defects : []).forEach((d) => {
    if (d && d.defectId != null) byId.set(Number(d.defectId), d)
  })
  const out = []
  ;(Array.isArray(links) ? links : []).forEach((e) => {
    if (!e) return
    if (String(e.linkType || 'FS').toUpperCase() !== 'FS') return
    const from = byId.get(Number(e.fromDefectId))
    const to = byId.get(Number(e.toDefectId))
    if (!from || !to) return
    const fe = startOfDay(parsePlanDate(from.planEndTime))
    const ts = startOfDay(parsePlanDate(to.planStartTime))
    if (!fe || !ts) return
    /* 后继开始应 ≥ 前置结束日的下一天（含首尾工期语义下允许同日结束/次日开始） */
    const earliest = addDays(fe, 1)
    if (earliest && ts.getTime() < earliest.getTime()) {
      out.push({ fromDefectId: Number(e.fromDefectId), toDefectId: Number(e.toDefectId) })
    }
  })
  return out
}
