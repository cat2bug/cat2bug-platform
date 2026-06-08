/**
 * Element UI el-dropdown 统一键盘行为（含 split-button）：
 * - split-button：整颗按钮一个焦点环 / 一个 Tab 停靠点
 * - 鼠标或键盘展开后默认高亮首项；菜单内 ↑↓ 选择；Enter 确认；Esc 关闭
 * - Enter / Space / ↓ 在焦点位于触发器上时展开菜单
 */

import { hasLayoutNavDropdownSession } from '@/utils/layout-nav-dropdown-kbd'
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'
import { findTopFormDrawerVm } from '@/utils/defect-drawer-shortcuts'

export const SPLIT_DROPDOWN_HOST_CLASS = 'cat2bug-split-dropdown-kbd'
export const DROPDOWN_HOST_SELECTOR = '.el-dropdown'
export const SPLIT_DROPDOWN_FOCUS_CLASS = 'cat2bug-split-dropdown-focus-target'
export const SPLIT_DROPDOWN_HOST_FOCUSED_CLASS = 'cat2bug-split-dropdown-host-focused'
export const SPLIT_DROPDOWN_ITEM_FOCUSED_CLASS = 'cat2bug-split-dropdown-item-focused'
/** 与 list-query-keyboard-nav 工具栏 ←/→ 导航共用 */
export const SPLIT_DROPDOWN_TOOLBAR_NAV_CLASS = 'list-query-toolbar-nav-focused'

const boundHosts = new WeakMap()
let globalKeydownBound = false

function isElDropdownVm(vm) {
  if (!vm || !vm.$options) return false
  const name = vm.$options.name || vm.$options.componentName
  return name === 'ElDropdown'
}

export function getDropdownVm(hostEl) {
  if (!hostEl) return null
  let el = hostEl
  while (el) {
    const vm = el.__vue__
    if (vm && isElDropdownVm(vm)) return vm
    el = el.parentElement
  }
  return null
}

function findDropdownTrigger(hostEl) {
  if (!hostEl) return null
  const direct = hostEl.querySelector(
    ':scope > button, :scope > .el-button, :scope > .el-dropdown-link, :scope > span, :scope > div'
  )
  if (direct && !direct.classList.contains('el-dropdown-menu')) return direct
  return hostEl.querySelector('button, .el-dropdown-link, [role="button"]')
}

export function ensureSplitDropdownFocusTarget(hostEl) {
  if (!hostEl) return null
  const existing = hostEl.querySelector(`.${SPLIT_DROPDOWN_FOCUS_CLASS}`)
  if (existing) return existing

  const group = hostEl.querySelector('.el-button-group')
  if (group) {
    group.classList.add(SPLIT_DROPDOWN_FOCUS_CLASS)
    group.querySelectorAll('button').forEach((btn) => {
      btn.setAttribute('tabindex', '-1')
    })
    group.setAttribute('tabindex', '0')
    group.setAttribute('role', 'button')
    return group
  }

  const trigger = findDropdownTrigger(hostEl)
  if (trigger) {
    trigger.classList.add(SPLIT_DROPDOWN_FOCUS_CLASS)
    if (!trigger.hasAttribute('tabindex')) {
      trigger.setAttribute('tabindex', '0')
    }
    if (!trigger.getAttribute('role')) {
      trigger.setAttribute('role', 'button')
    }
    return trigger
  }

  hostEl.classList.add(SPLIT_DROPDOWN_FOCUS_CLASS)
  hostEl.setAttribute('tabindex', '0')
  hostEl.setAttribute('role', 'button')
  return hostEl
}

export function getSplitDropdownFocusTarget(hostEl) {
  if (!hostEl) return null
  const existing = hostEl.querySelector(`.${SPLIT_DROPDOWN_FOCUS_CLASS}`)
  if (existing) return existing
  return ensureSplitDropdownFocusTarget(hostEl)
}

export function isSplitDropdownHost(el) {
  if (!el || !el.classList) return false
  return el.classList.contains('el-dropdown') ||
    el.classList.contains(SPLIT_DROPDOWN_HOST_CLASS)
}

export function resolveSplitDropdownHost(el) {
  if (!el) return null
  if (isSplitDropdownHost(el)) return el
  if (!el.closest) return null
  return el.closest(DROPDOWN_HOST_SELECTOR) ||
    el.closest(`.${SPLIT_DROPDOWN_HOST_CLASS}`)
}

export function shouldSkipDropdownHost(hostEl) {
  if (!hostEl) return true
  return hostEl.hasAttribute('data-cat2bug-dropdown-kbd-skip')
}

export function isSplitDropdownFocusTargetMarked(focusTarget) {
  if (!focusTarget) return false
  if (focusTarget.classList.contains(SPLIT_DROPDOWN_TOOLBAR_NAV_CLASS)) return true
  const active = document.activeElement
  return active === focusTarget || focusTarget.contains(active)
}

export function clearSplitDropdownHostFocusVisual(hostEl) {
  if (!hostEl) return
  hostEl.classList.remove(SPLIT_DROPDOWN_HOST_FOCUSED_CLASS)
  const target = getSplitDropdownFocusTarget(hostEl)
  if (target) target.classList.remove(SPLIT_DROPDOWN_HOST_FOCUSED_CLASS)
}

export function clearAllSplitDropdownHostFocusVisuals(containerEl) {
  if (!containerEl || !containerEl.querySelectorAll) return
  containerEl.querySelectorAll(DROPDOWN_HOST_SELECTOR).forEach(clearSplitDropdownHostFocusVisual)
}

export function isFocusOnSplitDropdownHost(hostEl) {
  if (!hostEl) return false
  const focusTarget = getSplitDropdownFocusTarget(hostEl)
  if (!focusTarget) return false
  return isSplitDropdownFocusTargetMarked(focusTarget) ||
    hostEl.classList.contains(SPLIT_DROPDOWN_HOST_FOCUSED_CLASS)
}

/** 当前应响应键盘的下拉宿主（含已展开菜单、工具栏导航焦点） */
export function resolveActiveSplitDropdownHost(rootEl) {
  const scope = rootEl && rootEl.querySelectorAll ? rootEl : document

  const openHosts = scope.querySelectorAll(DROPDOWN_HOST_SELECTOR)
  for (let i = 0; i < openHosts.length; i++) {
    if (isSplitDropdownOpen(openHosts[i])) return openHosts[i]
  }

  const marked = scope.querySelector(
    `.${SPLIT_DROPDOWN_FOCUS_CLASS}.${SPLIT_DROPDOWN_TOOLBAR_NAV_CLASS}`
  )
  if (marked) {
    const host = resolveSplitDropdownHost(marked)
    if (host) return host
  }
  const active = document.activeElement
  if (active) {
    const fromActive = resolveSplitDropdownHost(active)
    if (fromActive) return fromActive
  }
  for (let i = 0; i < openHosts.length; i++) {
    const target = getSplitDropdownFocusTarget(openHosts[i])
    if (target && isSplitDropdownFocusTargetMarked(target)) return openHosts[i]
  }
  return null
}

function queryDropdownMenuItems(dropdownVm) {
  if (!dropdownVm || !dropdownVm.visible) return []
  const menu = dropdownVm.dropdownElm || dropdownVm.popperElm
  if (!menu) return []
  return Array.from(menu.querySelectorAll('.el-dropdown-menu__item:not(.is-disabled)'))
    .filter((el) => {
      if (!el.isConnected) return false
      const rect = el.getBoundingClientRect()
      return rect.width > 0 || rect.height > 0
    })
}

export function getSplitDropdownMenuItems(dropdownVm) {
  return queryDropdownMenuItems(dropdownVm)
}

function clearMenuFocusMarks(dropdownVm) {
  const menu = dropdownVm && (dropdownVm.dropdownElm || dropdownVm.popperElm)
  if (!menu) return
  menu.querySelectorAll(`.el-dropdown-menu__item.${SPLIT_DROPDOWN_ITEM_FOCUSED_CLASS}`).forEach((el) => {
    el.classList.remove(SPLIT_DROPDOWN_ITEM_FOCUSED_CLASS)
  })
}

function syncMenuFocusMark(dropdownVm, index) {
  clearMenuFocusMarks(dropdownVm)
  const items = getSplitDropdownMenuItems(dropdownVm)
  const el = items[index]
  if (el) el.classList.add(SPLIT_DROPDOWN_ITEM_FOCUSED_CLASS)
}

function focusMenuItem(dropdownVm, index, state) {
  const items = getSplitDropdownMenuItems(dropdownVm)
  if (!items.length) return false
  const i = Math.max(0, Math.min(index, items.length - 1))
  if (state.menuIndex === i && document.activeElement === items[i]) {
    syncMenuFocusMark(dropdownVm, i)
    return false
  }
  if (typeof dropdownVm.removeTabindex === 'function') {
    dropdownVm.removeTabindex()
  }
  if (typeof dropdownVm.resetTabindex === 'function') {
    dropdownVm.resetTabindex(items[i])
  }
  items[i].focus()
  state.menuIndex = i
  syncMenuFocusMark(dropdownVm, i)
  return true
}

function resetMenuKeyboardState(state) {
  state.menuIndex = 0
}

/** 供页面级键盘导航复用：创建下拉菜单索引状态 */
export function createDropdownMenuKeyboardState() {
  return { menuIndex: 0 }
}

export function resetDropdownMenuKeyboardState(state) {
  resetMenuKeyboardState(state)
}

/** 聚焦指定下标菜单项（仅 .el-dropdown-menu__item，不含分隔线等） */
export function focusDropdownMenuItem(dropdownVm, index, state) {
  return focusMenuItem(dropdownVm, index, state)
}

export function getDropdownMenuIndex(dropdownVm, state) {
  return getMenuIndex(dropdownVm, state)
}

/** 下拉展开后聚焦首项（DOM 就绪前自动重试） */
export function focusInitialDropdownMenuItem(hostEl, dropdownVm, state) {
  return focusInitialMenuItem(hostEl, dropdownVm, state)
}

export function clearDropdownMenuFocusMarks(dropdownVm) {
  clearMenuFocusMarks(dropdownVm)
}

function focusInitialMenuItem(hostEl, dropdownVm, state, attempt = 0) {
  if (!dropdownVm || !dropdownVm.visible) return false
  resetMenuKeyboardState(state)
  const items = getSplitDropdownMenuItems(dropdownVm)
  if (items.length) {
    focusMenuItem(dropdownVm, 0, state)
    return true
  }
  if (attempt >= 12) return false
  setTimeout(() => focusInitialMenuItem(hostEl, dropdownVm, state, attempt + 1), 30)
  return false
}

function getMenuIndex(dropdownVm, state) {
  const items = getSplitDropdownMenuItems(dropdownVm)
  if (!items.length) return 0
  const active = document.activeElement
  const idx = items.indexOf(active)
  if (idx >= 0) return idx
  return Math.min(state.menuIndex || 0, items.length - 1)
}

function triggerSplitDropdownOpen(hostEl, dropdownVm) {
  if (!dropdownVm) return false
  if (dropdownVm.visible) return true
  if (dropdownVm.timeout) clearTimeout(dropdownVm.timeout)
  // 键盘展开：跳过 hover 默认 250ms 延迟
  dropdownVm.visible = true
  return true
}

export function openSplitDropdown(hostEl, state = {}) {
  if (shouldBlockToolbarSplitDropdownOpen(hostEl)) return Promise.resolve(false)
  bindSplitDropdownHost(hostEl)
  const dropdownVm = getDropdownVm(hostEl)
  if (!dropdownVm) return Promise.resolve(false)
  if (dropdownVm.visible) {
    return Promise.resolve(focusMenuItem(dropdownVm, state.menuIndex || 0, state))
  }
  triggerSplitDropdownOpen(hostEl, dropdownVm)
  return new Promise((resolve) => {
    const tryFocus = (attempt = 0) => {
      if (!dropdownVm.visible) {
        if (attempt < 16) {
          setTimeout(() => tryFocus(attempt + 1), 50)
        } else {
          resolve(false)
        }
        return
      }
      if (focusInitialMenuItem(hostEl, dropdownVm, state)) {
        resolve(true)
        return
      }
      if (attempt < 16) {
        setTimeout(() => tryFocus(attempt + 1), 50)
      } else {
        resolve(false)
      }
    }
    tryFocus()
  })
}

function syncDropdownMenuPopperVisible(dropdownVm, visible) {
  if (!dropdownVm) return
  const menuEl = dropdownVm.dropdownElm || dropdownVm.popperElm
  const menuVm = menuEl && menuEl.__vue__
  if (menuVm && Object.prototype.hasOwnProperty.call(menuVm, 'showPopper')) {
    menuVm.showPopper = visible
  }
}

function forceCloseSplitDropdown(hostEl, dropdownVm) {
  dropdownVm = dropdownVm || getDropdownVm(hostEl)
  if (!dropdownVm) return
  if (dropdownVm.timeout) {
    clearTimeout(dropdownVm.timeout)
    dropdownVm.timeout = null
  }
  dropdownVm.visible = false
  syncDropdownMenuPopperVisible(dropdownVm, false)
  clearMenuFocusMarks(dropdownVm)
  try {
    if (typeof dropdownVm.removeTabindex === 'function') {
      dropdownVm.removeTabindex()
    }
  } catch (err) {
    // ignore tabindex cleanup when trigger/menu DOM is not ready
  }
}

export function closeSplitDropdown(hostEl) {
  forceCloseSplitDropdown(hostEl)
}

export function isSplitDropdownOpen(hostEl) {
  const dropdownVm = getDropdownVm(hostEl)
  return !!(dropdownVm && dropdownVm.visible)
}

function isSplitDropdownOpenKey(key) {
  return key === 'Enter' || key === ' ' || key === 'Spacebar' ||
    key === 'ArrowDown' || key === 'Down'
}

function onHostKeydown(e, hostEl, state) {
  const dropdownVm = getDropdownVm(hostEl)
  const key = e.key
  if (dropdownVm && dropdownVm.visible) return
  if (!isSplitDropdownOpenKey(key)) return
  e.preventDefault()
  e.stopPropagation()
  const target = getSplitDropdownFocusTarget(hostEl)
  if (target && document.activeElement !== target) {
    try {
      target.focus({ preventScroll: false })
    } catch (err) {
      target.focus()
    }
  }
  openSplitDropdown(hostEl, state)
}

function stopMenuKeyEvent(e) {
  e.preventDefault()
  e.stopPropagation()
  if (typeof e.stopImmediatePropagation === 'function') {
    e.stopImmediatePropagation()
  }
}

function activateSplitDropdownMenuItem(hostEl, state, item) {
  const dropdownVm = getDropdownVm(hostEl)
  if (item) item.click()
  forceCloseSplitDropdown(hostEl, dropdownVm)
  const target = getSplitDropdownFocusTarget(hostEl)
  if (target) {
    try {
      target.focus({ preventScroll: false })
    } catch (err) {
      target.focus()
    }
  }
  resetMenuKeyboardState(state)
  if (dropdownVm && typeof dropdownVm.$nextTick === 'function') {
    dropdownVm.$nextTick(() => forceCloseSplitDropdown(hostEl, dropdownVm))
  }
}

function onMenuKeydown(e, hostEl, state) {
  if (e._cat2bugDropdownMenuKeyHandled) return
  const dropdownVm = getDropdownVm(hostEl)
  if (!dropdownVm || !dropdownVm.visible) return
  const items = getSplitDropdownMenuItems(dropdownVm)
  if (!items.length) return
  const key = e.key
  const isActivateKey = key === 'Enter' || key === ' ' || key === 'Spacebar'
  const isNavKey = key === 'ArrowDown' || key === 'Down' || key === 'ArrowUp' || key === 'Up' ||
    isActivateKey || key === 'Escape' || key === 'Esc'
  if (!isNavKey) return

  const focusedInMenu = items.indexOf(document.activeElement) >= 0
  const cur = focusedInMenu ? getMenuIndex(dropdownVm, state) : state.menuIndex

  if (key === 'ArrowDown' || key === 'Down') {
    stopMenuKeyEvent(e)
    e._cat2bugDropdownMenuKeyHandled = true
    if (!focusedInMenu) {
      focusMenuItem(dropdownVm, 0, state)
    } else if (cur < items.length - 1) {
      focusMenuItem(dropdownVm, cur + 1, state)
    }
    return
  }
  if (key === 'ArrowUp' || key === 'Up') {
    stopMenuKeyEvent(e)
    e._cat2bugDropdownMenuKeyHandled = true
    if (!focusedInMenu) {
      focusMenuItem(dropdownVm, 0, state)
      return
    }
    if (cur <= 0) {
      closeSplitDropdown(hostEl)
      const target = getSplitDropdownFocusTarget(hostEl)
      if (target) target.focus()
      resetMenuKeyboardState(state)
    } else {
      focusMenuItem(dropdownVm, cur - 1, state)
    }
    return
  }
  if (isActivateKey) {
    stopMenuKeyEvent(e)
    e._cat2bugDropdownMenuKeyHandled = true
    const idx = getMenuIndex(dropdownVm, state)
    activateSplitDropdownMenuItem(hostEl, state, items[idx])
    return
  }
  if (key === 'Escape' || key === 'Esc') {
    stopMenuKeyEvent(e)
    e._cat2bugDropdownMenuKeyHandled = true
    closeSplitDropdown(hostEl)
    const target = getSplitDropdownFocusTarget(hostEl)
    if (target) target.focus()
    resetMenuKeyboardState(state)
  }
}

function onDocumentKeydownForHost(e, hostEl, state) {
  const dropdownVm = getDropdownVm(hostEl)
  if (dropdownVm && dropdownVm.visible) {
    onMenuKeydown(e, hostEl, state)
    return
  }
  if (!isFocusOnSplitDropdownHost(hostEl)) return
  onHostKeydown(e, hostEl, state)
}

function onGlobalSplitDropdownKeydown(e) {
  if (hasLayoutNavDropdownSession()) return
  const host = resolveActiveSplitDropdownHost(document)
  if (!host) return
  if (shouldBlockToolbarSplitDropdownKeydown(host)) return
  let rec = boundHosts.get(host)
  if (!rec) {
    bindSplitDropdownHost(host)
    rec = boundHosts.get(host)
  }
  if (!rec) return
  onDocumentKeydownForHost(e, host, rec.state)
}

function watchDropdownVisible(hostEl, dropdownVm, state) {
  if (!dropdownVm || dropdownVm._cat2bugDropdownKbdVisibleUnwatch) return
  const unwatch = dropdownVm.$watch('visible', (visible) => {
    if (hasLayoutNavDropdownSession()) return
    if (visible) {
      dropdownVm.$nextTick(() => focusInitialMenuItem(hostEl, dropdownVm, state))
      return
    }
    clearMenuFocusMarks(dropdownVm)
    resetMenuKeyboardState(state)
  })
  dropdownVm._cat2bugDropdownKbdVisibleUnwatch = unwatch
}

function ensureGlobalSplitDropdownKeydown() {
  if (globalKeydownBound) return
  globalKeydownBound = true
  document.addEventListener('keydown', onGlobalSplitDropdownKeydown, true)
}

function onHostFocus(hostEl) {
  hostEl.classList.add(SPLIT_DROPDOWN_HOST_FOCUSED_CLASS)
  const target = getSplitDropdownFocusTarget(hostEl)
  if (target) target.classList.add(SPLIT_DROPDOWN_HOST_FOCUSED_CLASS)
}

function onHostBlur(hostEl) {
  hostEl.classList.remove(SPLIT_DROPDOWN_HOST_FOCUSED_CLASS)
  const target = getSplitDropdownFocusTarget(hostEl)
  if (target) target.classList.remove(SPLIT_DROPDOWN_HOST_FOCUSED_CLASS)
}

export function bindSplitDropdownHost(hostEl) {
  if (!hostEl || shouldSkipDropdownHost(hostEl)) return false
  if (boundHosts.has(hostEl)) {
    ensureGlobalSplitDropdownKeydown()
    return true
  }
  const focusTarget = ensureSplitDropdownFocusTarget(hostEl)
  const state = { menuIndex: 0 }
  const dropdownVm = getDropdownVm(hostEl)
  watchDropdownVisible(hostEl, dropdownVm, state)

  const rec = { state, focusTarget, onFocusIn: null, onFocusOut: null }
  if (focusTarget) {
    const onFocusIn = () => onHostFocus(hostEl)
    const onFocusOut = (e) => {
      const vm = getDropdownVm(hostEl)
      const menu = vm && (vm.dropdownElm || vm.popperElm)
      if (menu && e.relatedTarget && menu.contains(e.relatedTarget)) return
      if (hostEl.contains(e.relatedTarget)) return
      if (focusTarget.classList.contains(SPLIT_DROPDOWN_TOOLBAR_NAV_CLASS)) return
      onHostBlur(hostEl)
    }
    focusTarget.addEventListener('focusin', onFocusIn, true)
    focusTarget.addEventListener('focusout', onFocusOut, true)
    rec.onFocusIn = onFocusIn
    rec.onFocusOut = onFocusOut
  }
  boundHosts.set(hostEl, rec)
  ensureGlobalSplitDropdownKeydown()
  return true
}

export function unbindSplitDropdownHost(hostEl) {
  const rec = boundHosts.get(hostEl)
  if (!rec) return
  if (rec.focusTarget && rec.onFocusIn) {
    rec.focusTarget.removeEventListener('focusin', rec.onFocusIn, true)
    rec.focusTarget.removeEventListener('focusout', rec.onFocusOut, true)
  }
  const dropdownVm = getDropdownVm(hostEl)
  if (dropdownVm && dropdownVm._cat2bugDropdownKbdVisibleUnwatch) {
    dropdownVm._cat2bugDropdownKbdVisibleUnwatch()
    dropdownVm._cat2bugDropdownKbdVisibleUnwatch = null
  }
  boundHosts.delete(hostEl)
}

export function initSplitDropdownKbd(rootEl) {
  if (!rootEl || !rootEl.querySelectorAll) return
  rootEl.querySelectorAll(DROPDOWN_HOST_SELECTOR).forEach((host) => {
    bindSplitDropdownHost(host)
  })
}

export function destroySplitDropdownKbd(rootEl) {
  if (!rootEl || !rootEl.querySelectorAll) return
  rootEl.querySelectorAll(DROPDOWN_HOST_SELECTOR).forEach(unbindSplitDropdownHost)
}

/** 工具栏 / 快捷键：聚焦 split 宿主 */
export function focusSplitDropdownHost(hostEl, { toolbarNav = false } = {}) {
  if (!hostEl) return null
  bindSplitDropdownHost(hostEl)
  const target = getSplitDropdownFocusTarget(hostEl)
  if (!target) return null
  if (toolbarNav) {
    target.classList.add(SPLIT_DROPDOWN_TOOLBAR_NAV_CLASS)
    onHostFocus(hostEl)
  }
  try {
    target.focus({ preventScroll: false })
  } catch (e) {
    target.focus()
  }
  return target
}

function shouldBlockToolbarSplitDropdownOpen(hostEl) {
  if (isSplitDropdownInsideTopFormSurface(hostEl)) return false
  return hasBlockingUiLayer() || !!findTopFormDrawerVm()
}

function getTopFormSurfaceRootEl(topVm) {
  if (!topVm || !topVm.$el) return null
  const el = topVm.$el
  if (el.nodeType !== 1) return null
  if (el.classList && el.classList.contains('el-drawer__wrapper')) return el
  if (typeof el.closest === 'function') {
    const wrapper = el.closest('.el-drawer__wrapper')
    if (wrapper) return wrapper
  }
  return el
}

/** 抽屉/弹框内嵌工具栏下拉：允许在顶层表单表面内展开 */
function isSplitDropdownInsideTopFormSurface(hostEl) {
  if (!hostEl) return false
  const top = findTopFormDrawerVm()
  if (!top) return false
  const scope = getTopFormSurfaceRootEl(top)
  if (!scope || typeof scope.contains !== 'function') return false
  const host = resolveSplitDropdownHost(hostEl) || hostEl
  return scope.contains(host)
}

/** 菜单已展开时允许 ↑↓/Enter/Space/Esc，勿因 .el-dropdown-menu 遮挡层误拦截 */
function shouldBlockToolbarSplitDropdownKeydown(hostEl) {
  if (isSplitDropdownInsideTopFormSurface(hostEl)) return false
  if (findTopFormDrawerVm()) return true
  const dropdownVm = hostEl && getDropdownVm(hostEl)
  if (dropdownVm && dropdownVm.visible) return false
  return shouldBlockToolbarSplitDropdownOpen(hostEl)
}

function shouldBlockToolbarSplitDropdownShortcut(hostEl) {
  return shouldBlockToolbarSplitDropdownOpen(hostEl)
}

export function shouldBlockToolbarSplitDropdownInteraction(hostEl) {
  return shouldBlockToolbarSplitDropdownShortcut(hostEl)
}

/** 表单抽屉打开时：收起工具栏 split 下拉并移除焦点，避免 Space/Enter 误触展开 */
export function dismissToolbarSplitDropdownSessions(rootEl) {
  if (!rootEl || !rootEl.querySelectorAll) return
  rootEl.querySelectorAll(DROPDOWN_HOST_SELECTOR).forEach((host) => {
    closeSplitDropdown(host)
    clearSplitDropdownHostFocusVisual(host)
    const target = getSplitDropdownFocusTarget(host)
    if (target) {
      target.classList.remove(SPLIT_DROPDOWN_TOOLBAR_NAV_CLASS)
      if (typeof target.blur === 'function') target.blur()
    }
  })
}

/** 快捷键 / 工具栏导航：聚焦并展开 */
export async function shortcutOpenSplitDropdown(rootEl, selector) {
  if (shouldBlockToolbarSplitDropdownShortcut()) return false
  const host = rootEl.querySelector(selector)
  if (!host) return false
  focusSplitDropdownHost(host)
  await openSplitDropdown(host)
  return true
}

/** 工具栏 ←/→ 停在 split 上时：Enter / Space / ↓ 展开 */
export function tryOpenToolbarFocusedSplitDropdown(toolbarEl) {
  if (shouldBlockToolbarSplitDropdownShortcut()) return false
  if (!toolbarEl) return false
  const marked = toolbarEl.querySelector(
    `.${SPLIT_DROPDOWN_FOCUS_CLASS}.${SPLIT_DROPDOWN_TOOLBAR_NAV_CLASS}`
  )
  if (!marked) return false
  const host = resolveSplitDropdownHost(marked)
  if (!host) return false
  focusSplitDropdownHost(host, { toolbarNav: true })
  openSplitDropdown(host)
  return true
}

export function isFocusInSplitDropdownOverlay(target) {
  if (!target) return false
  return !!(target.closest && target.closest('.el-dropdown-menu'))
}
