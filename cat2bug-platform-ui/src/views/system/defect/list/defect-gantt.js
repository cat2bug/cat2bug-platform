/** 缺陷甘特：常量与投影（处理人镜像 / 交付物树序缩进） */

/** 每段 listDefect 条数（首屏与触底追加共用） */
export const GANTT_PAGE_SIZE = 50
/** 纵滚距底小于该值（px）时请求下一段 */
export const GANTT_LOAD_MORE_THRESHOLD_PX = 160
/** 首屏高度不足时自动续拉的段数上限，避免死循环 */
export const GANTT_VIEWPORT_AUTO_FETCH_MAX = 20
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
  /** 每档列宽 { day, week, month }（px） */
  COL_WIDTHS: 'defect.gantt.colWidths',
  /** 分组内缺陷行序 { [groupKey]: defectId[] } */
  ROW_ORDER: 'defect.gantt.rowOrder',
  /** 已折叠分组 key 列表 */
  COLLAPSED: 'defect.gantt.collapsedGroups',
  /** 未排期池高度 */
  UNSCHEDULED_HEIGHT: 'defect.gantt.unscheduledHeight',
  /** 未排期池是否折叠 */
  UNSCHEDULED_COLLAPSED: 'defect.gantt.unscheduledCollapsed',
  /** 图层开关 { deps, critical, load, baseline, minimap }；minimap 缺省 true */
  LAYERS: 'defect.gantt.layers',
  /** 缩略图面板位置 { x, y }（相对已排期主区左上） */
  MINIMAP_POS: 'defect.gantt.minimapPos'
})

/** 缩略图固定尺寸（px） */
export const GANTT_MINIMAP_WIDTH = 200
export const GANTT_MINIMAP_HEIGHT = 120

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

/** 日/周/月列宽默认值（px） */
export const GANTT_COL_WIDTH = Object.freeze({
  day: 36,
  week: 72,
  month: 96
})

/**
 * 档内列宽可调范围；越过 min/max（月宽阈值为 wideSwitch）触发升/降档。
 * day 已是最细：超过 max 仅钳制；month 已是最粗：低于 min 仅钳制。
 */
export const GANTT_COL_WIDTH_LIMITS = Object.freeze({
  day: Object.freeze({ min: 22, max: 64 }),
  week: Object.freeze({ min: 44, max: 110 }),
  month: Object.freeze({ min: 56, max: 160, wideSwitch: 140 })
})

/** @returns {{ day:number, week:number, month:number }} */
export function defaultColWidths() {
  return {
    day: GANTT_COL_WIDTH.day,
    week: GANTT_COL_WIDTH.week,
    month: GANTT_COL_WIDTH.month
  }
}

/**
 * 解析本地记忆的每档列宽；非法/缺省回退默认。
 * @param {unknown} raw
 * @returns {{ day:number, week:number, month:number }}
 */
export function parseStoredColWidths(raw) {
  const out = defaultColWidths()
  let parsed = raw
  if (typeof raw === 'string') {
    try {
      parsed = JSON.parse(raw)
    } catch (_) {
      return out
    }
  }
  if (!parsed || typeof parsed !== 'object') return out
  ;[GANTT_SCALE.DAY, GANTT_SCALE.WEEK, GANTT_SCALE.MONTH].forEach((k) => {
    const n = Number(parsed[k])
    if (Number.isFinite(n) && n > 0) out[k] = n
  })
  return out
}

/** 当前档宽阈：缩小过 min → 更粗尺度 */
export function coarserScale(scale) {
  if (scale === GANTT_SCALE.DAY) return GANTT_SCALE.WEEK
  if (scale === GANTT_SCALE.WEEK) return GANTT_SCALE.MONTH
  return null
}

/** 当前档宽阈：拉大过 max/wideSwitch → 更细尺度 */
export function finerScale(scale) {
  if (scale === GANTT_SCALE.MONTH) return GANTT_SCALE.WEEK
  if (scale === GANTT_SCALE.WEEK) return GANTT_SCALE.DAY
  return null
}

export function wideThreshold(scale) {
  const lim = GANTT_COL_WIDTH_LIMITS[scale]
  if (!lim) return GANTT_COL_WIDTH.day
  return lim.wideSwitch != null ? lim.wideSwitch : lim.max
}

export function resolveColWidth(scale, colWidths) {
  const map = colWidths && typeof colWidths === 'object' ? colWidths : null
  const n = map ? Number(map[scale]) : NaN
  if (Number.isFinite(n) && n > 0) return n
  return GANTT_COL_WIDTH[scale] || GANTT_COL_WIDTH.day
}

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

/**
 * 拉边改期。
 * - 传入整日（无时分）时保持「至少 1 个自然日」旧语义；
 * - 传入带时分或 durationDays 时支持 0.1 天小数工期。
 * @param {Date|string|number} startValue
 * @param {Date|string|number} endValue
 * @param {'start'|'end'} edge
 * @param {Date|string|number} dayDate 边沿对应日期/时间
 * @param {{ durationDays?: number }} [opts] 若给出则优先按工期重算该边
 */
export function resizePlanRange(startValue, endValue, edge, dayDate, opts = {}) {
  const start = parsePlanDate(startValue)
  const end = parsePlanDate(endValue)
  if (!start || !end) return null

  if (opts.durationDays != null && Number.isFinite(Number(opts.durationDays))) {
    const dur = snapDurationDays(Number(opts.durationDays), { min: 0.1, magnet: 0 })
    if (edge === 'end') {
      const ne = endFromDurationDays(start, dur)
      if (!ne) return null
      return {
        planStartTime: formatPlanDateTime(start),
        planEndTime: formatPlanDateTime(ne)
      }
    }
    if (edge === 'start') {
      const ns = startFromDurationDays(end, dur)
      if (!ns) return null
      return {
        planStartTime: formatPlanDateTime(ns),
        planEndTime: formatPlanDateTime(end)
      }
    }
  }

  const edgeAt = parsePlanDate(dayDate)
  if (!edgeAt) return null
  const fractional = planHasClockTime(edgeAt)

  if (edge === 'start') {
    let ns = fractional
      ? new Date(edgeAt.getTime())
      : (() => {
          const d = startOfDay(edgeAt)
          const t = new Date(d)
          t.setHours(start.getHours(), start.getMinutes(), start.getSeconds(), 0)
          return t
        })()
    const minEndGap = fractional ? 0.1 * DAY_MS : 0
    if (ns.getTime() > end.getTime() - minEndGap) {
      ns = fractional
        ? new Date(end.getTime() - 0.1 * DAY_MS)
        : (() => {
            const d = startOfDay(end)
            const t = new Date(d)
            t.setHours(start.getHours(), start.getMinutes(), start.getSeconds(), 0)
            return t
          })()
    }
    return {
      planStartTime: formatPlanDateTime(ns),
      planEndTime: formatPlanDateTime(end)
    }
  }
  if (edge === 'end') {
    let ne
    if (fractional) {
      ne = new Date(edgeAt.getTime())
      if (ne.getTime() < start.getTime() + 0.1 * DAY_MS) {
        ne = new Date(start.getTime() + 0.1 * DAY_MS)
      }
    } else {
      ne = endOfDay(edgeAt)
      const startDay = startOfDay(start)
      if (ne.getTime() < startDay.getTime()) {
        ne = endOfDay(startDay)
      }
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
/**
 * @param {object[]} defects
 * @param {string} groupBy
 * @param {Record<string|number, string>} [modulePathById]
 * @param {{ handlerIds?: Array<number|string>|number|string|null }} [opts]
 *   handlerIds：通用查询里的处理人过滤；有值时按处理人分组只投影这些处理人行，避免镜像到未筛选成员
 */
export function projectGanttRows(defects, groupBy, modulePathById, opts = {}) {
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
  return { groups: projectByHandler(scheduled, normalizeHandlerFilterIds(opts.handlerIds)), unscheduled }
}

/** @param {Array<number|string>|number|string|null|undefined} handleBy */
export function normalizeHandlerFilterIds(handleBy) {
  if (handleBy == null || handleBy === '') return null
  const arr = Array.isArray(handleBy) ? handleBy : [handleBy]
  const ids = []
  for (let i = 0; i < arr.length; i++) {
    const n = Number(arr[i])
    if (Number.isFinite(n) && !ids.includes(n)) ids.push(n)
  }
  return ids.length ? ids : null
}

function projectByHandler(defects, handlerFilterIds) {
  const filterSet =
    Array.isArray(handlerFilterIds) && handlerFilterIds.length
      ? new Set(handlerFilterIds.map(Number))
      : null
  /** @type {Map<string, GanttGroup>} */
  const map = new Map()
  for (let i = 0; i < defects.length; i++) {
    const defect = defects[i]
    const handlers = resolveHandlers(defect)
    if (!handlers.length) continue
    const mirrored = handlers.length > 1
    for (let h = 0; h < handlers.length; h++) {
      const handler = handlers[h]
      if (filterSet && !filterSet.has(Number(handler.userId))) continue
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

/**
 * 按尺度平移轴边界（unitDelta&lt;0 往过去，&gt;0 往未来）。
 * @returns {Date|null}
 */
export function shiftAxisBound(date, scale, unitDelta) {
  const n = Number(unitDelta)
  if (!Number.isFinite(n) || n === 0) return startOfDay(date)
  if (scale === GANTT_SCALE.WEEK) {
    const w = startOfWeek(date)
    return w ? addDays(w, Math.round(n) * 7) : null
  }
  if (scale === GANTT_SCALE.MONTH) {
    const m = startOfMonth(date)
    if (!m) return null
    return new Date(m.getFullYear(), m.getMonth() + Math.round(n), 1)
  }
  return addDays(date, Math.round(n))
}

/** 无限滚动一次延展的列数（约覆盖 1.5～2 屏） */
export function axisExtendChunkUnits(scale, colWidth, viewportPx) {
  const col = Number(colWidth) > 0 ? Number(colWidth) : GANTT_COL_WIDTH[scale] || 36
  const view = Number(viewportPx) > 0 ? Number(viewportPx) : 800
  const need = Math.ceil(view / col) + 4
  if (scale === GANTT_SCALE.MONTH) return Math.max(6, Math.min(36, need))
  if (scale === GANTT_SCALE.WEEK) return Math.max(12, Math.min(52, need))
  return Math.max(30, Math.min(120, need))
}

function resolveAxisCol(scale, colWidth) {
  const n = Number(colWidth)
  if (Number.isFinite(n) && n > 0) return n
  return GANTT_COL_WIDTH[scale] || GANTT_COL_WIDTH.day
}

/** 每「自然日」对应的轴像素（日=列宽；周/月按格内天数均分） */
export function pxPerDay(scale, colWidth) {
  const col = resolveAxisCol(scale, colWidth)
  if (scale === GANTT_SCALE.WEEK) return col / 7
  if (scale === GANTT_SCALE.MONTH) return col / 31
  return col
}

/**
 * 工期天数吸附：步进 0.1；靠近整数天时磁吸到整天。
 * @param {number} days
 * @param {{ min?: number, magnet?: number }} [opts]
 */
export function snapDurationDays(days, opts = {}) {
  const min = opts.min != null ? opts.min : 0.1
  const magnet = opts.magnet != null ? opts.magnet : 0.12
  if (!Number.isFinite(days)) return min
  let d = Math.round(days * 10) / 10
  if (d < min) d = min
  const nearest = Math.round(d)
  if (nearest >= 1 && Math.abs(d - nearest) <= magnet) {
    return nearest
  }
  return d
}

function planHasClockTime(d) {
  if (!d) return false
  return !!(d.getHours() || d.getMinutes() || d.getSeconds() || d.getMilliseconds())
}

/** 条几何用的结束瞬时：23:59:59 视为次日 0 点（整日条铺满格） */
function barExclusiveEnd(end) {
  const e = parsePlanDate(end)
  if (!e) return null
  if (e.getHours() === 23 && e.getMinutes() === 59) {
    return addDays(startOfDay(e), 1)
  }
  return e
}

/**
 * 由开始 + 工期天数得到结束时间（整天 → 末日 23:59:59；小数 → start+duration）。
 * @param {Date|string|number} startValue
 * @param {number} durationDays
 */
export function endFromDurationDays(startValue, durationDays) {
  const start = parsePlanDate(startValue)
  if (!start) return null
  const w = snapDurationDays(durationDays, { min: 0.1, magnet: 0 })
  if (Math.abs(w - Math.round(w)) < 1e-9) {
    const n = Math.max(1, Math.round(w))
    return endOfDay(addDays(startOfDay(start), n - 1))
  }
  return new Date(start.getTime() + w * DAY_MS)
}

/**
 * 由结束 + 工期天数得到开始时间。
 * @param {Date|string|number} endValue
 * @param {number} durationDays
 */
export function startFromDurationDays(endValue, durationDays) {
  const end = parsePlanDate(endValue)
  if (!end) return null
  const w = snapDurationDays(durationDays, { min: 0.1, magnet: 0 })
  if (Math.abs(w - Math.round(w)) < 1e-9) {
    const n = Math.max(1, Math.round(w))
    const endDay = end.getHours() === 23 && end.getMinutes() === 59 ? startOfDay(end) : startOfDay(end)
    return startOfDay(addDays(endDay, -(n - 1)))
  }
  return new Date(end.getTime() - w * DAY_MS)
}

export function dateToOffsetPx(date, rangeStart, scale, colWidth) {
  const d = parsePlanDate(date)
  const start = startOfDay(rangeStart)
  if (!d || !start) return 0
  const col = resolveAxisCol(scale, colWidth)
  if (scale === GANTT_SCALE.WEEK) {
    const days = (d.getTime() - start.getTime()) / DAY_MS
    return (days / 7) * col
  }
  if (scale === GANTT_SCALE.MONTH) {
    const months =
      (d.getFullYear() - start.getFullYear()) * 12 +
      (d.getMonth() - start.getMonth()) +
      (d.getDate() - 1) / 31 +
      (d.getHours() * 3600 + d.getMinutes() * 60 + d.getSeconds()) / (31 * 86400)
    return months * col
  }
  /* 日尺度含日内小数，支持 0.1 天条宽 */
  const days = (d.getTime() - start.getTime()) / DAY_MS
  return days * col
}

export function offsetPxToDate(offsetPx, rangeStart, scale, colWidth) {
  const start = startOfDay(rangeStart)
  if (!start) return null
  const col = resolveAxisCol(scale, colWidth)
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

/**
 * 轴偏移 → 日期时间（可含日内小数；可选 0.1 天步进 + 整天磁吸）。
 * @param {number} offsetPx
 * @param {Date|string|number} rangeStart
 * @param {string} scale
 * @param {number} [colWidth]
 * @param {{ snapTenths?: boolean, magnet?: number }} [opts]
 */
export function offsetPxToDateTime(offsetPx, rangeStart, scale, colWidth, opts = {}) {
  const start = startOfDay(rangeStart)
  if (!start) return null
  const ppd = pxPerDay(scale, colWidth)
  if (!(ppd > 0)) return offsetPxToDate(offsetPx, rangeStart, scale, colWidth)
  let days = offsetPx / ppd
  if (opts.snapTenths) {
    days = snapDurationDays(days, { min: 0, magnet: opts.magnet != null ? opts.magnet : 0.12 })
  }
  return new Date(start.getTime() + days * DAY_MS)
}

export function barGeometry(defect, rangeStart, scale, colWidth) {
  const s = parsePlanDate(defect.planStartTime)
  const e = parsePlanDate(defect.planEndTime)
  if (!s || !e) return null
  const col = resolveAxisCol(scale, colWidth)
  const x = dateToOffsetPx(s, rangeStart, scale, colWidth)
  const eEx = barExclusiveEnd(e)
  const x2 = dateToOffsetPx(eEx || e, rangeStart, scale, colWidth)
  const raw = x2 - x
  /*
   * 日尺度：保底 0.1 天宽便于点选/拉边；整日无时分时仍至少一列。
   * 周/月：按真实比例，勿保底整列。
   */
  let minPx = 4
  if (scale === GANTT_SCALE.DAY) {
    minPx = planHasClockTime(s) || planHasClockTime(e) ? col * 0.1 : col
  }
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
 * @param {number} [colWidth] 当前档列宽（可变）
 * @returns {{ left:number, width:number, level:1|2, count:number }[]}
 */
export function buildHandlerLoadTrackSegments(
  handlerId,
  defects,
  rangeStart,
  scale,
  threshold = 1,
  axisWidthPx = Infinity,
  colWidth
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
    let left = dateToOffsetPx(new Date(r.startMs), rangeStart, scale, colWidth)
    let right = dateToOffsetPx(new Date(r.endMs), rangeStart, scale, colWidth)
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
