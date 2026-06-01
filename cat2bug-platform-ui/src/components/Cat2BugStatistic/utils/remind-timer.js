/** @typedef {'once'|'daily'|'weekly'|'monthly'} ScheduleType */

export const SOUND_OPTIONS = [
  { id: 'bell-soft', file: 'bell-soft.mp3' },
  { id: 'chime', file: 'chime.mp3' },
  { id: 'tick', file: 'tick.mp3' }
]

/** 到点后默认重复播放时长（毫秒） */
export const DEFAULT_RING_DURATION_MS = 60000

/** 秒级 tick 下允许判定的到点窗口（毫秒） */
export const FIRE_WINDOW_MS = 2000

export function newTimerId() {
  return `t_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`
}

export function defaultTimer() {
  const now = new Date()
  const date = `${now.getFullYear()}-${pad2(now.getMonth() + 1)}-${pad2(now.getDate())}`
  return {
    id: newTimerId(),
    label: '',
    enabled: true,
    sound: 'bell-soft',
    firedOnce: false,
    schedule: { type: 'daily', time: '09:00', date, weekday: 1, day: 1 }
  }
}

function pad2(n) {
  return String(n).padStart(2, '0')
}

function parseTime(timeStr) {
  if (!timeStr || typeof timeStr !== 'string') {
    return { hours: 9, minutes: 0 }
  }
  const parts = timeStr.split(':')
  return {
    hours: Math.min(23, Math.max(0, parseInt(parts[0], 10) || 0)),
    minutes: Math.min(59, Math.max(0, parseInt(parts[1], 10) || 0))
  }
}

export function startOfDay(d) {
  const x = new Date(d)
  x.setHours(0, 0, 0, 0)
  return x
}

function setTimeOnDate(baseDate, timeStr) {
  const { hours, minutes } = parseTime(timeStr)
  const x = new Date(baseDate)
  x.setHours(hours, minutes, 0, 0)
  return x
}

function lastDayOfMonth(year, monthIndex) {
  return new Date(year, monthIndex + 1, 0).getDate()
}

function effectiveMonthlyDay(year, monthIndex, day) {
  const d = Math.max(1, Math.min(31, parseInt(day, 10) || 1))
  return Math.min(d, lastDayOfMonth(year, monthIndex))
}

/** weekday: 1=周一 … 7=周日 */
function jsDayFromWeekday(weekday) {
  const w = parseInt(weekday, 10) || 1
  return w === 7 ? 0 : w
}

/**
 * @param {object} schedule
 * @param {Date} now
 * @param {{ enabled?: boolean, firedOnce?: boolean }} timer
 * @returns {Date|null}
 */
export function computeNextFireAt(schedule, now = new Date(), timer = {}) {
  if (!schedule || timer.enabled === false) {
    return null
  }
  const type = schedule.type || 'daily'
  const time = schedule.time || '09:00'

  if (type === 'once') {
    if (timer.firedOnce) {
      return null
    }
    const dateStr = schedule.date
    if (!dateStr) {
      return null
    }
    const at = setTimeOnDate(new Date(`${dateStr}T00:00:00`), time)
    return at > now ? at : null
  }

  if (type === 'daily') {
    let candidate = setTimeOnDate(startOfDay(now), time)
    if (candidate <= now) {
      candidate = setTimeOnDate(startOfDay(new Date(now.getTime() + 86400000)), time)
    }
    return candidate
  }

  if (type === 'weekly') {
    const targetDow = jsDayFromWeekday(schedule.weekday)
    for (let i = 0; i < 8; i++) {
      const day = new Date(now)
      day.setDate(now.getDate() + i)
      if (day.getDay() === targetDow) {
        const candidate = setTimeOnDate(startOfDay(day), time)
        if (candidate > now) {
          return candidate
        }
      }
    }
    const nextWeek = new Date(now)
    nextWeek.setDate(now.getDate() + 7)
    return setTimeOnDate(startOfDay(nextWeek), time)
  }

  if (type === 'monthly') {
    const dayNum = parseInt(schedule.day, 10) || 1
    for (let mOffset = 0; mOffset < 14; mOffset++) {
      const y = now.getFullYear()
      const mo = now.getMonth() + mOffset
      const year = y + Math.floor(mo / 12)
      const monthIndex = ((mo % 12) + 12) % 12
      const effDay = effectiveMonthlyDay(year, monthIndex, dayNum)
      const candidate = setTimeOnDate(new Date(year, monthIndex, effDay), time)
      if (candidate > now) {
        return candidate
      }
    }
    return null
  }

  return null
}

function isWithinFireWindow(now, fireAt) {
  const diff = now.getTime() - fireAt.getTime()
  return diff >= 0 && diff < FIRE_WINDOW_MS
}

/**
 * 检测当前时刻是否应触发报时（与 computeNextFireAt 不同：基于「本周期触发点」判定）
 * @param {object} timer
 * @param {Date} now
 * @returns {{ fireKey: string, fireAt: Date }|null}
 */
export function detectTimerFire(timer, now = new Date()) {
  if (!timer || timer.enabled === false) {
    return null
  }
  const schedule = timer.schedule || {}
  const type = schedule.type || 'daily'
  const time = schedule.time || '09:00'

  if (type === 'once') {
    if (timer.firedOnce || !schedule.date) {
      return null
    }
    const fireAt = setTimeOnDate(new Date(`${schedule.date}T00:00:00`), time)
    if (isWithinFireWindow(now, fireAt)) {
      return { fireKey: `${timer.id}_${fireAt.getTime()}`, fireAt }
    }
    return null
  }

  if (type === 'daily') {
    const fireAt = setTimeOnDate(startOfDay(now), time)
    if (isWithinFireWindow(now, fireAt)) {
      return { fireKey: `${timer.id}_${fireAt.getTime()}`, fireAt }
    }
    return null
  }

  if (type === 'weekly') {
    const targetDow = jsDayFromWeekday(schedule.weekday)
    if (now.getDay() !== targetDow) {
      return null
    }
    const fireAt = setTimeOnDate(startOfDay(now), time)
    if (isWithinFireWindow(now, fireAt)) {
      return { fireKey: `${timer.id}_${fireAt.getTime()}`, fireAt }
    }
    return null
  }

  if (type === 'monthly') {
    const dayNum = parseInt(schedule.day, 10) || 1
    const effDay = effectiveMonthlyDay(now.getFullYear(), now.getMonth(), dayNum)
    if (now.getDate() !== effDay) {
      return null
    }
    const fireAt = setTimeOnDate(startOfDay(now), time)
    if (isWithinFireWindow(now, fireAt)) {
      return { fireKey: `${timer.id}_${fireAt.getTime()}`, fireAt }
    }
    return null
  }

  return null
}

/**
 * @param {number} ms
 * @returns {string}
 */
export function formatCountdown(ms) {
  if (ms == null || ms <= 0) {
    return '00:00:00'
  }
  const totalSec = Math.floor(ms / 1000)
  const days = Math.floor(totalSec / 86400)
  const h = Math.floor((totalSec % 86400) / 3600)
  const m = Math.floor((totalSec % 3600) / 60)
  const s = totalSec % 60
  const hms = `${pad2(h)}:${pad2(m)}:${pad2(s)}`
  if (days > 0) {
    return `${days} ${hms}`
  }
  return hms
}

export function soundUrl(soundId) {
  const opt = SOUND_OPTIONS.find(o => o.id === soundId) || SOUND_OPTIONS[0]
  const base = (process.env.BASE_URL || '/').replace(/\/?$/, '/')
  return `${base}sounds/${opt.file}`
}

/**
 * @param {Array} timers
 * @param {Date} now
 * @returns {Array<{ timer, nextAt, remainingMs, countdownText }>}
 */
export function buildTimerRows(timers, now = new Date()) {
  if (!Array.isArray(timers)) {
    return []
  }
  return timers
    .filter(t => t && t.enabled !== false)
    .map(timer => {
      const nextAt = computeNextFireAt(timer.schedule, now, timer)
      const remainingMs = nextAt ? nextAt.getTime() - now.getTime() : null
      return {
        timer,
        nextAt,
        remainingMs,
        countdownText: remainingMs != null && remainingMs > 0
          ? formatCountdown(remainingMs)
          : null
      }
    })
    .filter(row => row.nextAt && row.remainingMs > 0)
    .sort((a, b) => a.remainingMs - b.remainingMs)
}

/** 自然日差：end - today（end 为计划结束日 0 点） */
export function calendarDaysUntil(endDate, today = startOfDay(new Date())) {
  if (!endDate) {
    return null
  }
  const end = startOfDay(endDate)
  const msPerDay = 86400000
  return Math.round((end.getTime() - today.getTime()) / msPerDay)
}
