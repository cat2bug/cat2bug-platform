/**
 * 面板/抽屉内快捷键徽标：视口检测、数字优先分配、字母池。
 */

const BLOCKED_LETTERS = new Set(['A', 'C', 'V', 'X', 'Z'])

const LETTER_POOL_FALLBACK = [
  'D', 'F', 'G', 'S', 'E', 'M', 'B', 'J', 'K', 'P', 'U', 'Y', 'I', 'H', 'L', 'N', 'O', 'R', 'T', 'W', 'Q'
]

export function isVisibleEl(el) {
  return !!(el && el.offsetParent !== null)
}

export function getViewportRect(scrollContainer, fallbackEl) {
  if (scrollContainer) return scrollContainer.getBoundingClientRect()
  if (fallbackEl) return fallbackEl.getBoundingClientRect()
  return {
    top: 0,
    left: 0,
    bottom: window.innerHeight,
    right: window.innerWidth
  }
}

export function isElementInViewportRect(el, viewportRect, minVisiblePx = 8) {
  if (!el || !isVisibleEl(el)) return false
  const rect = el.getBoundingClientRect()
  if (rect.height <= 0 && rect.width <= 0) return false
  const overlapTop = Math.max(rect.top, viewportRect.top)
  const overlapBottom = Math.min(rect.bottom, viewportRect.bottom)
  return overlapBottom - overlapTop >= minVisiblePx
}

/** 为 N 个可见项分配键：先 1–9、0，再用未占用字母 */
export function assignDigitThenLetterKeys(count, reserved = {}) {
  const keys = []
  const used = new Set()
  const take = (k) => {
    if (!k || used.has(k) || reserved[k]) return false
    used.add(k)
    keys.push(k)
    return true
  }
  for (let d = 1; d <= 9 && keys.length < count; d++) take(String(d))
  if (keys.length < count) take('0')
  for (const k of LETTER_POOL_FALLBACK) {
    if (keys.length >= count) break
    if (BLOCKED_LETTERS.has(k)) continue
    take(k)
  }
  for (let c = 65; c <= 90 && keys.length < count; c++) {
    const k = String.fromCharCode(c)
    if (BLOCKED_LETTERS.has(k)) continue
    take(k)
  }
  while (keys.length < count) keys.push('')
  return keys
}

/** 工具栏按钮：按首选字母分配，冲突时回退字母池 */
export function assignToolbarHintKeys(items, preferredMap = {}) {
  const map = {}
  const used = new Set()
  const take = (k) => {
    if (!k || used.has(k) || BLOCKED_LETTERS.has(k)) return ''
    used.add(k)
    return k
  }
  items.forEach((item) => {
    let letter = take(preferredMap[item.key])
    if (!letter) {
      for (const k of LETTER_POOL_FALLBACK) {
        letter = take(k)
        if (letter) break
      }
    }
    if (!letter) {
      for (let c = 65; c <= 90; c++) {
        letter = take(String.fromCharCode(c))
        if (letter) break
      }
    }
    if (letter) map[item.key] = letter
  })
  return map
}

export function isScrollableEl(el) {
  if (!el) return false
  const oy = window.getComputedStyle(el).overflowY
  if (oy !== 'auto' && oy !== 'scroll' && oy !== 'overlay') return false
  return el.scrollHeight > el.clientHeight + 2
}

export function findScrollContainer(startEl) {
  let el = startEl
  while (el && el !== document.body) {
    if (isScrollableEl(el)) return el
    el = el.parentElement
  }
  return null
}

/** 从内容根节点解析抽屉滚动容器（body 为子节点，不能用 closest 从 wrapper 向上找） */
export function resolveDrawerScrollContainer(contentRoot, drawerRoot) {
  if (contentRoot) {
    const body = contentRoot.closest('.el-drawer__body')
    if (body && isScrollableEl(body)) return body
    const found = findScrollContainer(contentRoot)
    if (found) return found
    if (body) return body
  }
  if (drawerRoot && typeof drawerRoot.querySelector === 'function') {
    const body = drawerRoot.querySelector('.el-drawer__body')
    if (body && isScrollableEl(body)) return body
    if (body) return body
  }
  return null
}

export function isVisibleLayer(el) {
  if (!el || !el.getClientRects) return false
  const st = window.getComputedStyle(el)
  if (st.display === 'none' || st.visibility === 'hidden') return false
  return el.getClientRects().length > 0
}

/** 当前组件是否为最上层可见抽屉（排除子对话框） */
export function isTopDrawerVm(vm) {
  if (!vm || !vm.$el) return false
  if (Array.from(document.querySelectorAll('.el-dialog__wrapper')).some(isVisibleLayer)) return false
  if (Array.from(document.querySelectorAll('.el-message-box__wrapper')).some(isVisibleLayer)) return false
  const wrappers = []
  document.querySelectorAll('.el-drawer__wrapper').forEach((w) => {
    if (!isVisibleLayer(w)) return
    const z = parseInt(window.getComputedStyle(w).zIndex, 10) || 0
    wrappers.push({ w, z })
  })
  if (!wrappers.length) return false
  wrappers.sort((a, b) => b.z - a.z)
  const top = wrappers[0].w
  const content = typeof vm.getHandleKbdContentRoot === 'function'
    ? vm.getHandleKbdContentRoot()
    : null
  if (content && top.contains(content)) return true
  return top.contains(vm.$el)
}
