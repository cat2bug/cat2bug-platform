/**
 * 日期范围面板：按住 ⌘/Ctrl 在头部「上一年/月、下一年/月」按钮上显示快捷键徽标。
 */

const OVERLAY_ID = 'cat2bug-date-panel-kbd-overlay'

/** 面板打开后可直接按（无需修饰键）；按住 ⌘/Ctrl 时在按钮上显示 */
export const DATE_PANEL_NAV_KEYS = {
  prevYear: '-',
  prevMonth: ',',
  nextMonth: '.',
  nextYear: '='
}

/** keyCode → 面板导航方法（与 DATE_PANEL_NAV_KEYS 一致；勿用 [ ]，与 ⌘/Ctrl 后退/前进冲突） */
export const DATE_PANEL_NAV_KEY_CODES = {
  prevYear: 189,
  prevMonth: 188,
  nextMonth: 190,
  nextYear: 187
}

const NAV_BUTTON_SPECS = [
  { side: 'left', icon: 'el-icon-d-arrow-left', key: DATE_PANEL_NAV_KEYS.prevYear, method: 'leftPrevYear' },
  { side: 'left', icon: 'el-icon-arrow-left', key: DATE_PANEL_NAV_KEYS.prevMonth, method: 'leftPrevMonth' },
  { side: 'left', icon: 'el-icon-d-arrow-right', key: DATE_PANEL_NAV_KEYS.nextYear, method: 'leftNextYear', unlinkOnly: true },
  { side: 'left', icon: 'el-icon-arrow-right', key: DATE_PANEL_NAV_KEYS.nextMonth, method: 'leftNextMonth', unlinkOnly: true },
  { side: 'right', icon: 'el-icon-d-arrow-left', key: DATE_PANEL_NAV_KEYS.prevYear, method: 'rightPrevYear', unlinkOnly: true },
  { side: 'right', icon: 'el-icon-arrow-left', key: DATE_PANEL_NAV_KEYS.prevMonth, method: 'rightPrevMonth', unlinkOnly: true },
  { side: 'right', icon: 'el-icon-arrow-right', key: DATE_PANEL_NAV_KEYS.nextMonth, method: 'rightNextMonth' },
  { side: 'right', icon: 'el-icon-d-arrow-right', key: DATE_PANEL_NAV_KEYS.nextYear, method: 'rightNextYear' }
]

const NAV_KEYCODE_MAP = {
  [DATE_PANEL_NAV_KEY_CODES.prevYear]: 'leftPrevYear',
  [DATE_PANEL_NAV_KEY_CODES.prevMonth]: 'leftPrevMonth',
  [DATE_PANEL_NAV_KEY_CODES.nextMonth]: 'rightNextMonth',
  [DATE_PANEL_NAV_KEY_CODES.nextYear]: 'rightNextYear'
}

const NAV_KEYCHAR_MAP = {
  [DATE_PANEL_NAV_KEYS.prevYear]: 'leftPrevYear',
  [DATE_PANEL_NAV_KEYS.prevMonth]: 'leftPrevMonth',
  [DATE_PANEL_NAV_KEYS.nextMonth]: 'rightNextMonth',
  [DATE_PANEL_NAV_KEYS.nextYear]: 'rightNextYear'
}

let modifierHeld = false
let installed = false
let hintsObserverPaused = false
let hintsUpdateRafId = null

function isModifierKeyEvent(e) {
  if (!e) return false
  const k = e.key
  return k === 'Control' || k === 'Meta' || k === 'OS' ||
    e.keyCode === 17 || e.keyCode === 91 || e.keyCode === 93
}

function ensureOverlay() {
  let el = document.getElementById(OVERLAY_ID)
  if (!el) {
    el = document.createElement('div')
    el.id = OVERLAY_ID
    el.className = 'cat2bug-page-kbd-hint-layer cat2bug-date-panel-kbd-layer'
    el.setAttribute('aria-hidden', 'true')
    document.body.appendChild(el)
  }
  return el
}

function getVisibleRangePanel() {
  const panels = Array.from(document.querySelectorAll('.el-picker-panel.el-date-range-picker'))
  for (let i = 0; i < panels.length; i++) {
    const panel = panels[i]
    if (panel.getAttribute('aria-hidden') === 'true') continue
    const style = window.getComputedStyle(panel)
    if (style.display === 'none' || style.visibility === 'hidden') continue
    const rect = panel.getBoundingClientRect()
    if (rect.width > 0 && rect.height > 0) return panel
  }
  return null
}

function findHeaderButton(panelEl, side, iconClass) {
  const content = panelEl.querySelector(`.el-date-range-picker__content.is-${side}`)
  if (!content) return null
  const btn = content.querySelector(`.el-picker-panel__icon-btn.${iconClass}`)
  if (!btn || btn.disabled || btn.classList.contains('is-disabled')) return null
  if (btn.offsetParent === null && getComputedStyle(btn).display === 'none') return null
  return btn
}

/** 面板关闭或失焦时重置修饰键状态，避免 modifierHeld 残留触发 Observer 死循环 */
export function resetDatePickerPanelHintModifier() {
  modifierHeld = false
  hideDatePickerPanelHints()
}

export function hideDatePickerPanelHints() {
  const overlay = document.getElementById(OVERLAY_ID)
  if (!overlay || !overlay.firstChild) return
  hintsObserverPaused = true
  try {
    overlay.replaceChildren()
  } finally {
    hintsObserverPaused = false
  }
}

function renderDatePickerPanelHints() {
  if (!modifierHeld) {
    hideDatePickerPanelHints()
    return
  }
  const panelEl = getVisibleRangePanel()
  if (!panelEl) {
    hideDatePickerPanelHints()
    return
  }
  hintsObserverPaused = true
  try {
    const overlay = ensureOverlay()
    const badges = []
    NAV_BUTTON_SPECS.forEach((spec) => {
      const btn = findHeaderButton(panelEl, spec.side, spec.icon)
      if (!btn) return
      const rect = btn.getBoundingClientRect()
      if (rect.width <= 0 || rect.height <= 0) return
      const badge = document.createElement('span')
      badge.className = 'cat2bug-page-kbd-hint-float cat2bug-date-panel-kbd-hint'
      badge.textContent = spec.key
      badge.style.left = `${Math.round(rect.right - 10)}px`
      badge.style.top = `${Math.round(rect.top - 6)}px`
      badges.push(badge)
    })
    overlay.replaceChildren(...badges)
  } finally {
    hintsObserverPaused = false
  }
}

export function showDatePickerPanelHints() {
  renderDatePickerPanelHints()
}

function scheduleDatePickerPanelHintsUpdate() {
  if (hintsUpdateRafId != null) return
  hintsUpdateRafId = requestAnimationFrame(() => {
    hintsUpdateRafId = null
    if (hintsObserverPaused) return
    if (modifierHeld && getVisibleRangePanel()) renderDatePickerPanelHints()
    else hideDatePickerPanelHints()
  })
}

export function isDatePanelNavKeyEvent(event) {
  if (!event) return false
  const key = event.key
  if (key && NAV_KEYCHAR_MAP[key]) return true
  const keyCode = event.keyCode
  return keyCode != null && !!NAV_KEYCODE_MAP[keyCode]
}

export function runDatePanelNavByKeyCode(keyCode, panel, keyChar) {
  if (!panel) return false
  const method = (keyCode != null && NAV_KEYCODE_MAP[keyCode]) ||
    (keyChar != null && NAV_KEYCHAR_MAP[keyChar])
  if (!method || typeof panel[method] !== 'function') return false
  const btn = getNavButtonForMethod(panel, method)
  if (btn && (btn.disabled || btn.classList.contains('is-disabled'))) return false
  panel[method]()
  return true
}

function getNavButtonForMethod(panel, method) {
  const spec = NAV_BUTTON_SPECS.find((s) => s.method === method)
  if (!spec || !panel.$el) return null
  return findHeaderButton(panel.$el, spec.side, spec.icon)
}

export function installDatePickerPanelHints() {
  if (installed || typeof window === 'undefined') return
  installed = true

  window.addEventListener('keydown', (e) => {
    if (e.metaKey || e.ctrlKey) modifierHeld = true
    if (!getVisibleRangePanel()) {
      resetDatePickerPanelHintModifier()
      return
    }
    if (modifierHeld) scheduleDatePickerPanelHintsUpdate()
  }, true)

  window.addEventListener('keyup', (e) => {
    if (isModifierKeyEvent(e)) {
      resetDatePickerPanelHintModifier()
      return
    }
    if (!e.metaKey && !e.ctrlKey) {
      resetDatePickerPanelHintModifier()
    }
  }, true)

  window.addEventListener('blur', () => {
    resetDatePickerPanelHintModifier()
  })

  const observer = new MutationObserver(() => {
    if (hintsObserverPaused) return
    if (!getVisibleRangePanel()) {
      if (modifierHeld) resetDatePickerPanelHintModifier()
      else hideDatePickerPanelHints()
      return
    }
    scheduleDatePickerPanelHintsUpdate()
  })
  if (document.body) {
    observer.observe(document.body, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ['class', 'style', 'aria-hidden']
    })
  }
}
