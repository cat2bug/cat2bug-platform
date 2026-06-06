/**
 * ↑/↓ + Tab 方向意图（供 tab-direction 与 combo/upload Tab 处理器共享，避免循环依赖）。
 */

/** 方向键松开后仍认可「方向 + Tab」的窗口（毫秒） */
export const ARROW_TAB_WINDOW_MS = 800

let arrowUpHeld = false
let arrowDownHeld = false
/** 最近一次方向键意图：-1 上 / 1 下 / 0 无 */
let pendingTabDirection = 0
let pendingTabDirectionAt = 0

export function resetArrowTabIntent() {
  arrowUpHeld = false
  arrowDownHeld = false
  pendingTabDirection = 0
  pendingTabDirectionAt = 0
}

export function markArrowTabDirection(dir) {
  pendingTabDirection = dir
  pendingTabDirectionAt = Date.now()
  if (dir < 0) arrowUpHeld = true
  if (dir > 0) arrowDownHeld = true
}

export function releaseArrowKey(key) {
  if (key === 'ArrowUp') arrowUpHeld = false
  if (key === 'ArrowDown') arrowDownHeld = false
}

export function hasArrowTabIntent() {
  if (arrowUpHeld || arrowDownHeld) return true
  if (pendingTabDirection === 0) return false
  return Date.now() - pendingTabDirectionAt <= ARROW_TAB_WINDOW_MS
}

export function getArrowTabDirection() {
  if (arrowUpHeld || pendingTabDirection === -1) return -1
  if (arrowDownHeld || pendingTabDirection === 1) return 1
  return 0
}

export function clearArrowTabIntentAfterTab() {
  resetArrowTabIntent()
}

export function cancelPendingArrowTabIntent() {
  pendingTabDirection = 0
  pendingTabDirectionAt = 0
}

/** Shift+Tab 或 ↑+Tab → 反向；↓+Tab / 普通 Tab → 正向 */
export function resolveTabReverseFromEvent(e) {
  if (e && e.shiftKey) return true
  return getArrowTabDirection() < 0
}
