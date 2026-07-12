/**
 * 项目信息「更改封面」popover：I 展开；网格内 ↑↓←→；Enter/Space 选择；首行 ↑ / Esc 关闭。
 */

import {
  closePopover,
  isPopoverOpen,
  isVisiblePopperEl,
  queryVisiblePoppersByClass
} from '@/adapters/element-plus/popover-adapter'
import {
  isOnFirstRow,
  moveStatisticGridIndex
} from '@/utils/statistic-grid-kbd'

export const PROJECT_ICON_POPOVER_HOST_CLASS = 'project-icon-popover-kbd'
export const PROJECT_ICON_ITEM_FOCUS_CLASS = 'project-icon-item-kbd-current'
export const PROJECT_ICON_POPPER_CLASS = 'project-icon-popper'
export const PROJECT_ICON_CHANGE_HINT_CLASS = 'project-icon-hint-change'

const HOST_VM_KEY = '__cat2bugProjectIconVm'
const bindings = new WeakMap()
/** @type {Set<object>} */
const registeredVms = new Set()
let globalKeydownHandler = null

function stopKeyEvent(e) {
  e.preventDefault()
  e.stopPropagation()
  if (typeof e.stopImmediatePropagation === 'function') {
    e.stopImmediatePropagation()
  }
}

function isOpenKey(key) {
  return key === 'ArrowDown' || key === 'Down' ||
    key === 'Enter' || key === ' ' || key === 'Spacebar'
}

function isActivateKey(key) {
  return key === 'Enter' || key === ' ' || key === 'Spacebar'
}

function directionFromKey(key) {
  if (key === 'ArrowLeft' || key === 'Left') return 'left'
  if (key === 'ArrowRight' || key === 'Right') return 'right'
  if (key === 'ArrowUp' || key === 'Up') return 'up'
  if (key === 'ArrowDown' || key === 'Down') return 'down'
  return null
}

function isEditableFocus() {
  const el = document.activeElement
  if (!el || el === document.body) return false
  const tag = el.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return true
  return !!el.isContentEditable
}

/** DOM 中是否存在可见的项目封面 popover（teleport 到 body） */
export function isAnyProjectIconPopoverOpenInDom() {
  return queryVisiblePoppersByClass(PROJECT_ICON_POPPER_CLASS).length > 0
}

export function getProjectIconPopoverVm(vm) {
  return vm && vm.$refs && vm.$refs.projectIconPopover
}

export function getProjectIconPopperEl(vm) {
  const pop = getProjectIconPopoverVm(vm)
  if (pop) {
    const popperRef = pop.popperRef
    const popperFromRef = popperRef && (popperRef.value || popperRef)
    const el = popperFromRef || pop.popperElm || (pop.$refs && pop.$refs.popper) || null
    if (el && isVisiblePopperEl(el)) return el
  }
  const visible = queryVisiblePoppersByClass(PROJECT_ICON_POPPER_CLASS)
  return visible.length ? visible[visible.length - 1] : null
}

export function isProjectIconPopoverVisible(vm) {
  if (isAnyProjectIconPopoverOpenInDom()) return true
  if (vm && vm.projectIconPopperVisible === true) return true
  return isPopoverOpen(getProjectIconPopoverVm(vm))
}

function linkVmToHost(vm) {
  if (!vm || !vm.$el || !vm.$el.querySelector) return
  const host = vm.$el.querySelector(`.${PROJECT_ICON_POPOVER_HOST_CLASS}`)
  if (host) host[HOST_VM_KEY] = vm
}

function resolveRegisteredProjectIconVm() {
  const active = Array.from(registeredVms).filter((vm) => vm && !vm._inactive && !vm._isDestroyed)
  for (let i = active.length - 1; i >= 0; i--) {
    const vm = active[i]
    if (vm.$el && document.body.contains(vm.$el)) return vm
  }
  const host = document.querySelector(`.${PROJECT_ICON_POPOVER_HOST_CLASS}`)
  if (host && host[HOST_VM_KEY]) return host[HOST_VM_KEY]
  return active.length ? active[active.length - 1] : null
}

function resolveActiveProjectIconVm() {
  if (!isAnyProjectIconPopoverOpenInDom()) {
    return resolveRegisteredProjectIconVm()
  }
  const active = Array.from(registeredVms).filter((vm) => vm && !vm._inactive && !vm._isDestroyed)
  for (let i = active.length - 1; i >= 0; i--) {
    const vm = active[i]
    if (vm.projectIconPopperVisible === true) return vm
    const pop = getProjectIconPopoverVm(vm)
    if (pop && pop.showPopper) return vm
  }
  const host = document.querySelector(`.${PROJECT_ICON_POPOVER_HOST_CLASS}`)
  if (host && host[HOST_VM_KEY]) return host[HOST_VM_KEY]
  return active.length ? active[active.length - 1] : null
}

/** 重新挂到 window 末尾，确保在 defect-drawer 全局 Esc 之后仍能收到按键 */
function bumpGlobalKeydownHandler() {
  if (globalKeydownHandler) {
    window.removeEventListener('keydown', globalKeydownHandler, true)
  }
  globalKeydownHandler = (e) => {
    const vm = resolveActiveProjectIconVm()
    if (!vm) return
    handleDocumentKeydown(e, vm)
  }
  window.addEventListener('keydown', globalKeydownHandler, true)
}

export function getProjectIconPopoverItems(popperEl) {
  if (!popperEl || !popperEl.querySelectorAll) return []
  return Array.from(popperEl.querySelectorAll('.project-icon-item')).filter(isVisiblePopperEl)
}

export function focusProjectIconTrigger(vm) {
  const btn = vm.$refs.projectIconTrigger
  const el = btn && (btn.$el || btn)
  if (!el || typeof el.focus !== 'function') return
  try {
    el.focus({ preventScroll: false })
  } catch (e) {
    el.focus()
  }
}

export function clearProjectIconPopoverFocus(vm) {
  const popper = getProjectIconPopperEl(vm)
  if (!popper) return
  popper.querySelectorAll(`.${PROJECT_ICON_ITEM_FOCUS_CLASS}`).forEach((el) => {
    el.classList.remove(PROJECT_ICON_ITEM_FOCUS_CLASS)
  })
}

export function syncProjectIconItemFocus(vm, index) {
  clearProjectIconPopoverFocus(vm)
  const items = getProjectIconPopoverItems(getProjectIconPopperEl(vm))
  const item = items[index]
  if (!item) return false
  item.classList.add(PROJECT_ICON_ITEM_FOCUS_CLASS)
  if (!item.hasAttribute('tabindex')) {
    item.setAttribute('tabindex', '-1')
  }
  try {
    item.focus({ preventScroll: false })
  } catch (e) {
    item.focus()
  }
  const rec = bindings.get(vm)
  if (rec) {
    rec.state.itemIndex = index
    rec.state.inGrid = true
  }
  return true
}

export function focusProjectIconItemWhenReady(vm, itemIndex = 0, attempt = 0) {
  if (!isProjectIconPopoverVisible(vm)) return
  vm.$nextTick(() => {
    vm.$nextTick(() => {
      if (!isProjectIconPopoverVisible(vm)) return
      const items = getProjectIconPopoverItems(getProjectIconPopperEl(vm))
      if (items.length) {
        syncProjectIconItemFocus(vm, Math.min(itemIndex, items.length - 1))
        return
      }
      if (attempt < 12) {
        setTimeout(() => focusProjectIconItemWhenReady(vm, itemIndex, attempt + 1), 40)
      }
    })
  })
}

export function openProjectIconPopover(vm, options = {}) {
  vm.projectIconPopperVisible = true
  bumpGlobalKeydownHandler()
  if (options.focusTrigger) {
    vm.$nextTick(() => focusProjectIconTrigger(vm))
    return
  }
  const itemIndex = options.focusItemIndex != null ? options.focusItemIndex : 0
  focusProjectIconItemWhenReady(vm, itemIndex)
}

export function closeProjectIconPopover(vm) {
  closePopover(getProjectIconPopoverVm(vm))
  vm.projectIconPopperVisible = false
  clearProjectIconPopoverFocus(vm)
  const rec = bindings.get(vm)
  if (rec) {
    rec.state.itemIndex = -1
    rec.state.inGrid = false
  }
}

function isProjectIconTriggerFocused(vm) {
  const btn = vm.$refs.projectIconTrigger
  const el = btn && (btn.$el || btn)
  return !!(el && document.activeElement === el)
}

function activateProjectIconItem(vm, index) {
  const items = getProjectIconPopoverItems(getProjectIconPopperEl(vm))
  const item = items[index]
  if (!item) return
  const image = item.querySelector('.el-image')
  if (image && typeof image.click === 'function') {
    image.click()
    return
  }
  const uploadBtn = item.querySelector('.update-button-add')
  if (uploadBtn && typeof uploadBtn.click === 'function') {
    uploadBtn.click()
  }
}

function handleDocumentKeydown(e, vm) {
  if (!vm || vm._inactive || vm._isDestroyed) return
  const key = e.key
  const visible = isProjectIconPopoverVisible(vm)
  const triggerFocused = isProjectIconTriggerFocused(vm)
  const rec = bindings.get(vm)
  const state = rec ? rec.state : { itemIndex: -1, inGrid: false }

  if (!visible) {
    if ((key === 'i' || key === 'I') && !e.metaKey && !e.ctrlKey && !e.altKey && !e.shiftKey) {
      if (e.isComposing || isEditableFocus()) return
      stopKeyEvent(e)
      openProjectIconPopover(vm, { focusItemIndex: 0 })
      return
    }
    if (!triggerFocused || !isOpenKey(key)) return
    stopKeyEvent(e)
    openProjectIconPopover(vm, { focusItemIndex: 0 })
    return
  }

  if (key === 'Escape' || key === 'Esc') {
    stopKeyEvent(e)
    closeProjectIconPopover(vm)
    focusProjectIconTrigger(vm)
    return
  }

  const items = getProjectIconPopoverItems(getProjectIconPopperEl(vm))
  const dir = directionFromKey(key)
  const isNav = dir || isActivateKey(key)
  if (!isNav) return
  if (!items.length) return

  stopKeyEvent(e)

  let idx = state.inGrid ? state.itemIndex : -1

  if (dir === 'down') {
    if (idx < 0) {
      syncProjectIconItemFocus(vm, 0)
      return
    }
    const next = moveStatisticGridIndex(items, idx, 'down')
    if (next !== idx) syncProjectIconItemFocus(vm, next)
    return
  }

  if (dir === 'up') {
    if (idx < 0) {
      syncProjectIconItemFocus(vm, 0)
      return
    }
    if (isOnFirstRow(items, idx)) {
      closeProjectIconPopover(vm)
      focusProjectIconTrigger(vm)
      return
    }
    const next = moveStatisticGridIndex(items, idx, 'up')
    syncProjectIconItemFocus(vm, next)
    return
  }

  if (dir === 'left' || dir === 'right') {
    if (idx < 0) {
      syncProjectIconItemFocus(vm, 0)
      return
    }
    const next = moveStatisticGridIndex(items, idx, dir)
    syncProjectIconItemFocus(vm, next)
    return
  }

  if (isActivateKey(key)) {
    if (idx < 0) {
      syncProjectIconItemFocus(vm, 0)
      return
    }
    activateProjectIconItem(vm, idx)
  }
}

function bindProjectIconPopoverWatch(vm, state) {
  const pop = getProjectIconPopoverVm(vm)
  if (pop && !pop._cat2bugProjectIconKbdWatch) {
    pop._cat2bugProjectIconKbdWatch = pop.$watch('showPopper', (visible) => {
      if (visible) {
        bumpGlobalKeydownHandler()
        focusProjectIconItemWhenReady(vm, state.itemIndex >= 0 ? state.itemIndex : 0)
        return
      }
      clearProjectIconPopoverFocus(vm)
      state.itemIndex = -1
      state.inGrid = false
    })
  }
  if (!vm._cat2bugProjectIconVisibleWatch) {
    vm._cat2bugProjectIconVisibleWatch = vm.$watch('projectIconPopperVisible', (visible) => {
      if (visible) {
        bumpGlobalKeydownHandler()
        focusProjectIconItemWhenReady(vm, state.itemIndex >= 0 ? state.itemIndex : 0)
        return
      }
      clearProjectIconPopoverFocus(vm)
      state.itemIndex = -1
      state.inGrid = false
    })
  }
}

function ensureProjectIconPopoverWatch(vm, state, attempt = 0) {
  bindProjectIconPopoverWatch(vm, state)
  linkVmToHost(vm)
  if (!getProjectIconPopoverVm(vm) && attempt < 12) {
    setTimeout(() => ensureProjectIconPopoverWatch(vm, state, attempt + 1), 40)
  }
}

export function initProjectIconPopoverKbd(vm) {
  if (!vm) return
  registeredVms.add(vm)
  if (!bindings.has(vm)) {
    bindings.set(vm, { state: { itemIndex: -1, inGrid: false } })
  }
  bumpGlobalKeydownHandler()
  vm.$nextTick(() => ensureProjectIconPopoverWatch(vm, bindings.get(vm).state))
}

export function destroyProjectIconPopoverKbd(vm) {
  registeredVms.delete(vm)
  const rec = bindings.get(vm)
  if (rec) bindings.delete(vm)
  const pop = getProjectIconPopoverVm(vm)
  if (pop && pop._cat2bugProjectIconKbdWatch) {
    pop._cat2bugProjectIconKbdWatch()
    pop._cat2bugProjectIconKbdWatch = null
  }
  if (vm._cat2bugProjectIconVisibleWatch) {
    vm._cat2bugProjectIconVisibleWatch()
    vm._cat2bugProjectIconVisibleWatch = null
  }
  const host = vm.$el && vm.$el.querySelector(`.${PROJECT_ICON_POPOVER_HOST_CLASS}`)
  if (host && host[HOST_VM_KEY] === vm) {
    delete host[HOST_VM_KEY]
  }
}

export function shortcutOpenProjectIconPopover(vm) {
  openProjectIconPopover(vm, { focusItemIndex: 0 })
}
