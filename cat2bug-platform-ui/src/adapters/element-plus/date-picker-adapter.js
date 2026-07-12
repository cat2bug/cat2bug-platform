/**
 * Element Plus date-picker DOM / component instance adapter.
 * Business keyboard logic lives in utils/composables; only EP-specific access here.
 */

export const DATE_PICKER_SELECTORS = {
  editor: '.el-date-editor',
  activeEditor: '.el-range-editor.is-active, .el-date-editor.is-active',
  panel: '.el-picker-panel',
  rangeInput: '.el-range-input'
}

export function isVisibleEl(el) {
  if (!el || !el.getBoundingClientRect) return false
  if (el.getAttribute('aria-hidden') === 'true') return false
  const style = window.getComputedStyle(el)
  if (style.display === 'none' || style.visibility === 'hidden') return false
  const rect = el.getBoundingClientRect()
  return rect.width > 0 && rect.height > 0
}

export function queryActiveDateEditors(root = document) {
  return root.querySelectorAll(DATE_PICKER_SELECTORS.activeEditor)
}

export function queryDatePanels(root = document) {
  return Array.from(root.querySelectorAll(DATE_PICKER_SELECTORS.panel)).filter(isVisibleEl)
}

export function isDatePickerEditorEl(el) {
  return !!(el && el.classList && el.classList.contains('el-date-editor'))
}

/** Resolve Element Plus date-picker component instance from a DOM node. */
export function resolveDatePickerComponent(fromEl) {
  if (!fromEl || !fromEl.closest) return null
  const editor = fromEl.closest(DATE_PICKER_SELECTORS.editor)
  if (!editor) return null

  const candidates = []
  if (editor.__vue__) candidates.push(editor.__vue__)
  let vm = fromEl.__vue__
  while (vm) {
    if (candidates.indexOf(vm) === -1) candidates.push(vm)
    vm = vm.$parent
  }

  for (let i = 0; i < candidates.length; i++) {
    const node = candidates[i]
    if (typeof node.handleFocus === 'function' && 'pickerVisible' in node) return node
  }
  return null
}

export function isDatePickerFocusEl(el) {
  return !!resolveDatePickerComponent(el)
}
