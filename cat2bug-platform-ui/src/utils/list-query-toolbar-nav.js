/**
 * 列表页查询区 ↔ 右侧工具栏焦点桥接与工具栏内 ←/→ 导航。
 */

import {
  ensureSplitDropdownFocusTarget,
  resolveSplitDropdownHost,
  SPLIT_DROPDOWN_HOST_CLASS
} from '@/utils/split-dropdown-kbd'
import { isDatePickerPanelOpen } from '@/utils/date-picker-kbd'

const QUERY_NAV_OVERLAY_SELECTORS = ['.el-picker-panel', '.select-project-member-popover']

function isVisibleOverlay(el) {
  if (!el || !el.getBoundingClientRect) return false
  if (el.getAttribute('aria-hidden') === 'true') return false
  const style = window.getComputedStyle(el)
  if (style.display === 'none' || style.visibility === 'hidden') return false
  const rect = el.getBoundingClientRect()
  return rect.width > 0 && rect.height > 0
}

/** 查询区 ←/→ 导航：日期面板或成员选择浮层实际可见时拦截 */
export function isQueryNavOverlayOpen(selectors = QUERY_NAV_OVERLAY_SELECTORS) {
  if (isDatePickerPanelOpen()) return true
  for (let s = 0; s < selectors.length; s++) {
    const nodes = document.querySelectorAll(selectors[s])
    for (let i = 0; i < nodes.length; i++) {
      if (isVisibleOverlay(nodes[i])) return true
    }
  }
  return false
}

const FOCUSABLE_SELECTOR = [
  'button:not([tabindex="-1"])',
  'a[href]',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  `[tabindex]:not([tabindex="-1"])`,
  `.${SPLIT_DROPDOWN_HOST_CLASS} .cat2bug-split-dropdown-focus-target`
].join(', ')

export function isVisibleFocusable(el, { allowDisabled = false } = {}) {
  if (!el || typeof el.getBoundingClientRect !== 'function') return false
  const disabled = !!(el.disabled || el.getAttribute('aria-disabled') === 'true')
  if (!allowDisabled && disabled) return false
  if (el.getAttribute('aria-hidden') === 'true') return false
  const rect = el.getBoundingClientRect()
  if (rect.width <= 0 && rect.height <= 0) return false
  if (getComputedStyle(el).visibility === 'hidden') return false
  if (allowDisabled && disabled) return true
  if (el.offsetParent === null && getComputedStyle(el).position !== 'fixed') return false
  return true
}

/** @returns {HTMLElement[]} 按视觉从左到右排序 */
export function getToolbarFocusables(containerEl) {
  if (!containerEl || typeof containerEl.querySelectorAll !== 'function') return []
  containerEl.querySelectorAll(`.${SPLIT_DROPDOWN_HOST_CLASS}`).forEach((host) => {
    ensureSplitDropdownFocusTarget(host)
  })
  const raw = Array.from(containerEl.querySelectorAll(FOCUSABLE_SELECTOR))
    .filter((el) => isVisibleFocusable(el, { allowDisabled: el.tagName === 'BUTTON' }))
  const result = []
  const addedSplit = new Set()
  for (const node of raw) {
    const host = resolveSplitDropdownHost(node)
    if (host) {
      const target = ensureSplitDropdownFocusTarget(host)
      if (target && !addedSplit.has(target)) {
        addedSplit.add(target)
        result.push(target)
      }
      continue
    }
    if (node.closest && node.closest(`.${SPLIT_DROPDOWN_HOST_CLASS}`)) continue
    result.push(node)
  }
  return result.sort((a, b) => {
    const ra = a.getBoundingClientRect()
    const rb = b.getBoundingClientRect()
    if (Math.abs(ra.left - rb.left) > 2) return ra.left - rb.left
    return ra.top - rb.top
  })
}

export function getFirstToolbarFocusable(containerEl) {
  const list = getToolbarFocusables(containerEl)
  return list[0] || null
}

export function getToolbarFocusableIndex(containerEl, el, markedClass = 'list-query-toolbar-nav-focused') {
  if (!containerEl) return -1
  const list = getToolbarFocusables(containerEl)
  const target = el || document.activeElement
  if (target) {
    for (let i = 0; i < list.length; i++) {
      const node = list[i]
      if (node === target || node.contains(target) || target.contains(node)) return i
    }
  }
  if (markedClass) {
    const marked = containerEl.querySelector(`.${markedClass}`)
    if (marked) {
      const idx = list.indexOf(marked)
      if (idx >= 0) return idx
    }
  }
  return -1
}

export function isFirstToolbarFocusable(containerEl, el) {
  return getToolbarFocusableIndex(containerEl, el) === 0
}

export function isFocusInToolbar(containerEl, target) {
  if (!containerEl || !target) return false
  return containerEl.contains(target)
}

export function focusToolbarAtIndex(containerEl, index) {
  const list = getToolbarFocusables(containerEl)
  if (index < 0 || index >= list.length) return { focused: false, el: null }
  const el = list[index]
  const disabled = !!(el.disabled || el.getAttribute('aria-disabled') === 'true')
  if (disabled) {
    return { focused: false, el, disabled: true }
  }
  try {
    el.focus({ preventScroll: false })
  } catch (e) {
    el.focus()
  }
  return { focused: true, el, disabled: false }
}
