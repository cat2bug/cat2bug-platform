/** 登录页老鼠方向键：统一解析 keydown/keyup（兼容 macOS 别名与 keyCode） */
export const ARROW_KEYS = ['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight']

const LEGACY_ARROW_TO_KEY = {
  Up: 'ArrowUp',
  Down: 'ArrowDown',
  Left: 'ArrowLeft',
  Right: 'ArrowRight'
}

const CODE_TO_ARROW_KEY = {
  ArrowUp: 'ArrowUp',
  ArrowDown: 'ArrowDown',
  ArrowLeft: 'ArrowLeft',
  ArrowRight: 'ArrowRight'
}

const KEY_CODE_TO_ARROW = {
  37: 'ArrowLeft',
  38: 'ArrowUp',
  39: 'ArrowRight',
  40: 'ArrowDown'
}

export const OPPOSITE_ARROW_KEYS = {
  ArrowUp: ['ArrowDown'],
  ArrowDown: ['ArrowUp'],
  ArrowLeft: ['ArrowRight'],
  ArrowRight: ['ArrowLeft']
}

export function resolveArrowKey(e) {
  if (!e) return null
  if (ARROW_KEYS.includes(e.key)) return e.key
  if (LEGACY_ARROW_TO_KEY[e.key]) return LEGACY_ARROW_TO_KEY[e.key]
  if (CODE_TO_ARROW_KEY[e.code]) return CODE_TO_ARROW_KEY[e.code]
  if (KEY_CODE_TO_ARROW[e.keyCode]) return KEY_CODE_TO_ARROW[e.keyCode]
  return null
}

export function isArrowKeyEvent(e) {
  return !!resolveArrowKey(e)
}

export function isModifierKeyEvent(e) {
  if (!e) return false
  const k = e.key
  return k === 'Control' || k === 'Meta' || k === 'OS' ||
    e.keyCode === 17 || e.keyCode === 91 || e.keyCode === 93
}

export function canKeyboardLock() {
  return typeof navigator !== 'undefined'
    && navigator.keyboard
    && typeof navigator.keyboard.lock === 'function'
}

export async function lockArrowKeys() {
  if (!canKeyboardLock()) return false
  try {
    await navigator.keyboard.lock(ARROW_KEYS)
    return true
  } catch (err) {
    return false
  }
}

export async function unlockArrowKeys() {
  if (!canKeyboardLock()) return
  try {
    await navigator.keyboard.unlock()
  } catch (err) {
    /* ignore */
  }
}

/**
 * 当 macOS 在 ⌘ 仍按住时丢失方向键 keyup，用 keydown 活动间隔推断「已松开」。
 * - 出现过 repeat：短间隔（默认 110ms）即可判定松开
 * - 仅单次 keydown：用较长 idle 间隔（默认 450ms），避免在系统 repeat 延迟前误判
 */
export function createArrowKeyReleasePoller({
  getHeldKeys,
  onRelease,
  repeatGapMs = 110,
  idleGapMs = 450
}) {
  const activity = new Map()
  let rafId = 0

  function touch(key, isRepeat) {
    const now = performance.now()
    const entry = activity.get(key) || { last: now, first: now, sawRepeat: false }
    entry.last = now
    if (isRepeat) entry.sawRepeat = true
    activity.set(key, entry)
    if (!rafId) rafId = requestAnimationFrame(tick)
  }

  function clear(key) {
    activity.delete(key)
    if (activity.size === 0 && rafId) {
      cancelAnimationFrame(rafId)
      rafId = 0
    }
  }

  function reset() {
    activity.clear()
    if (rafId) {
      cancelAnimationFrame(rafId)
      rafId = 0
    }
  }

  function tick(now) {
    rafId = requestAnimationFrame(tick)
    for (const key of getHeldKeys()) {
      const entry = activity.get(key)
      if (!entry) continue
      const gap = entry.sawRepeat ? repeatGapMs : idleGapMs
      if (now - entry.last > gap) {
        onRelease(key)
        clear(key)
      }
    }
    if (!getHeldKeys().size) reset()
  }

  return { touch, clear, reset }
}

/** 避免同一 keyup 在 window/document 重复触发 */
export function markArrowEventHandled(e) {
  if (!e || e.__cat2bugArrowHandled) return true
  e.__cat2bugArrowHandled = true
  return false
}
