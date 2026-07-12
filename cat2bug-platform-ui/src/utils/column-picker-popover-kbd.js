/** 与缺陷列表「显示字段」共用 popper 类名（dropdown-blur-close / 主题样式已注册） */
import {
  closePopover,
  findPopoverTriggerInContainer,
  isPopoverOpen,
  isVisiblePopperEl,
  openPopover,
  queryVisiblePoppersByClass,
  resolvePopoverComponent
} from '@/adapters/element-plus/popover-adapter'

export const COLUMN_PICKER_POPPER_CLASS = 'defect-column-picker-popover'

export const COLUMN_PICKER_ITEM_FOCUS_CLASS = 'defect-column-picker-item-focused'

const CHECKBOX_SELECTOR = '.defect-column-picker .el-checkbox:not(.is-disabled)'

export function resolveColumnPickerPopoverVm(el) {
  return resolvePopoverComponent(el)
}

export function openColumnPickerPopover(triggerEl) {
  return openPopover(resolvePopoverComponent(triggerEl))
}

export function closeColumnPickerPopover(triggerEl) {
  return closePopover(resolvePopoverComponent(triggerEl))
}

export function isColumnPickerPopoverOpen(triggerEl) {
  return isPopoverOpen(resolvePopoverComponent(triggerEl))
}

export function findColumnPickerTriggerInToolbar(toolbarEl, focusedEl) {
  return findPopoverTriggerInContainer(toolbarEl, focusedEl)
}

export function openColumnPickerFromToolbarFocus(toolbarEl, focusedEl) {
  const trigger = findColumnPickerTriggerInToolbar(toolbarEl, focusedEl)
  if (!trigger) return false
  return openColumnPickerPopover(trigger)
}

export function isColumnPickerOpenInToolbar(toolbarEl) {
  if (!toolbarEl || typeof toolbarEl.querySelectorAll !== 'function') return false
  const refs = toolbarEl.querySelectorAll('.el-popover__reference-wrapper, .el-popover__reference')
  for (let i = 0; i < refs.length; i++) {
    const trigger = refs[i].matches('button') ? refs[i] : refs[i].querySelector('button')
    if (trigger && isColumnPickerPopoverOpen(trigger)) return true
  }
  return false
}

export function isVisibleColumnPickerPopper(el) {
  return isVisiblePopperEl(el)
}

/** 当前可见的「显示字段」浮层（同时仅应有一个） */
export function getVisibleColumnPickerPopperEl() {
  return queryVisiblePoppersByClass(COLUMN_PICKER_POPPER_CLASS)[0] || null
}

export function getColumnPickerCheckboxEls(popperEl) {
  const popper = popperEl || getVisibleColumnPickerPopperEl()
  if (!popper) return []
  return Array.from(popper.querySelectorAll(CHECKBOX_SELECTOR))
}

export function clearColumnPickerFocusMarks(popperEl) {
  const popper = popperEl || getVisibleColumnPickerPopperEl()
  if (!popper) return
  popper.querySelectorAll(`.el-checkbox.${COLUMN_PICKER_ITEM_FOCUS_CLASS}`).forEach((el) => {
    el.classList.remove(COLUMN_PICKER_ITEM_FOCUS_CLASS)
  })
  popper.querySelectorAll(`${CHECKBOX_SELECTOR}[tabindex]`).forEach((el) => {
    el.removeAttribute('tabindex')
  })
}

/** 鼠标点开浮层时：去掉键盘高亮并移出 popper 内焦点 */
export function releaseColumnPickerMouseFocus(popperEl) {
  const popper = popperEl || getVisibleColumnPickerPopperEl()
  if (!popper) return
  clearColumnPickerFocusMarks(popper)
  const active = document.activeElement
  if (active && popper.contains(active) && typeof active.blur === 'function') {
    active.blur()
  }
}

export function syncColumnPickerFocusClass(popperEl, menuIndex) {
  clearColumnPickerFocusMarks(popperEl)
  const items = getColumnPickerCheckboxEls(popperEl)
  if (!items.length) return
  const i = Math.max(0, Math.min(menuIndex, items.length - 1))
  const el = items[i]
  if (el) el.classList.add(COLUMN_PICKER_ITEM_FOCUS_CLASS)
}

export function getColumnPickerMenuIndex(popperEl, menuIndex) {
  const items = getColumnPickerCheckboxEls(popperEl)
  if (!items.length) return 0
  const active = document.activeElement
  let node = active
  while (node) {
    const idx = items.indexOf(node)
    if (idx >= 0) return idx
    node = node.parentElement
  }
  return Math.min(menuIndex || 0, items.length - 1)
}

export function focusColumnPickerItem(popperEl, index, menuIndexRef = { value: 0 }) {
  const items = getColumnPickerCheckboxEls(popperEl)
  if (!items.length) return false
  const i = Math.max(0, Math.min(index, items.length - 1))
  const el = items[i]
  el.setAttribute('tabindex', '0')
  try {
    el.focus({ preventScroll: false })
  } catch (e) {
    el.focus()
  }
  if (typeof el.scrollIntoView === 'function') {
    el.scrollIntoView({ block: 'nearest' })
  }
  menuIndexRef.value = i
  syncColumnPickerFocusClass(popperEl, i)
  return true
}

export function moveColumnPickerItem(popperEl, delta, menuIndexRef) {
  const items = getColumnPickerCheckboxEls(popperEl)
  if (!items.length) return false
  const cur = getColumnPickerMenuIndex(popperEl, menuIndexRef.value)
  const next = cur + delta
  if (next < 0 || next >= items.length) return false
  return focusColumnPickerItem(popperEl, next, menuIndexRef)
}

export function toggleColumnPickerItem(popperEl, index, menuIndexRef) {
  const items = getColumnPickerCheckboxEls(popperEl)
  if (!items[index]) return
  const input = items[index].querySelector('input[type="checkbox"]')
  if (input) input.click()
  else items[index].click()
  if (menuIndexRef) {
    setTimeout(() => focusColumnPickerItem(popperEl, index, menuIndexRef), 0)
  }
}

export function isFocusInColumnPickerOverlay(target, popperEl) {
  if (!target) return false
  const popper = popperEl || getVisibleColumnPickerPopperEl()
  return !!(popper && popper.contains(target))
}

/**
 * @param {object} opts
 * @param {() => boolean} opts.isOpen
 * @param {() => HTMLElement|null} [opts.getPopperEl]
 * @param {{ value: number }} opts.menuIndexRef
 * @param {() => void} opts.close
 */
export function handleColumnPickerMenuKeydown(e, opts) {
  if (!opts.isOpen()) return false
  const popperEl = (opts.getPopperEl && opts.getPopperEl()) || getVisibleColumnPickerPopperEl()
  const key = e.key
  const focusedIn = isFocusInColumnPickerOverlay(document.activeElement, popperEl)

  if (key === 'ArrowDown' || key === 'Down') {
    e.preventDefault()
    e.stopPropagation()
    if (!focusedIn) {
      focusColumnPickerItem(popperEl, 0, opts.menuIndexRef)
    } else if (!moveColumnPickerItem(popperEl, 1, opts.menuIndexRef)) {
      const items = getColumnPickerCheckboxEls(popperEl)
      focusColumnPickerItem(popperEl, items.length - 1, opts.menuIndexRef)
    }
    return true
  }
  if (key === 'ArrowUp' || key === 'Up') {
    e.preventDefault()
    e.stopPropagation()
    if (!focusedIn) {
      focusColumnPickerItem(popperEl, 0, opts.menuIndexRef)
    } else if (getColumnPickerMenuIndex(popperEl, opts.menuIndexRef.value) <= 0) {
      opts.close()
    } else {
      moveColumnPickerItem(popperEl, -1, opts.menuIndexRef)
    }
    return true
  }
  if (key === ' ' || key === 'Spacebar' || e.code === 'Space' || key === 'Enter') {
    e.preventDefault()
    e.stopPropagation()
    const idx = getColumnPickerMenuIndex(popperEl, opts.menuIndexRef.value)
    toggleColumnPickerItem(popperEl, idx, opts.menuIndexRef)
    return true
  }
  if (key === 'Escape' || key === 'Esc') {
    e.preventDefault()
    e.stopPropagation()
    opts.close()
    return true
  }
  return false
}
