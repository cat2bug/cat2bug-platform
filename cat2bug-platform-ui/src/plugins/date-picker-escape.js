/**
 * Element UI 日期/时间选择器：下拉面板打开后按 Esc 关闭面板。
 * 焦点在 popper 面板内时 reference input 收不到 keydown，需捕获阶段全局处理。
 */

import { shortcutService } from '@/plugins/shortcut/service'
import { closeDatePickerPanelKeepFocus, resolveDatePickerVm } from '@/utils/date-picker-kbd'

function isEscapeKey(e) {
  return e && (e.key === 'Escape' || e.key === 'Esc' || e.keyCode === 27)
}

function isVisible(el) {
  if (!el || !el.getClientRects) return false
  const st = window.getComputedStyle(el)
  if (st.display === 'none' || st.visibility === 'hidden') return false
  return el.getClientRects().length > 0
}

function hasOpenPickerPanel() {
  return Array.from(document.querySelectorAll('.el-picker-panel')).some(isVisible)
}

function closeActiveElementPickers() {
  let closed = false
  document.querySelectorAll('.el-date-editor.is-active, .el-range-editor.is-active').forEach((el) => {
    const vm = resolveDatePickerVm(el) || el.__vue__
    if (!vm) return
    if (closeDatePickerPanelKeepFocus(vm)) {
      closed = true
      return
    }
    if (typeof vm.handleClose === 'function') {
      vm.handleClose()
      closed = true
    } else if (vm.pickerVisible) {
      vm.pickerVisible = false
      closed = true
    }
  })
  return closed
}

function onKeydown(e) {
  if (!isEscapeKey(e) || e.isComposing) return
  if (e.metaKey || e.ctrlKey || e.altKey) return
  if (shortcutService.palette && shortcutService.palette.open) return
  if (!hasOpenPickerPanel() &&
    !document.querySelector('.el-date-editor.is-active, .el-range-editor.is-active')) {
    return
  }
  if (closeActiveElementPickers()) {
    e.preventDefault()
    e.stopPropagation()
  }
}

const DatePickerEscapePlugin = {
  install() {
    window.addEventListener('keydown', onKeydown, true)
  }
}

export default DatePickerEscapePlugin
