/** 报时提醒弹框表格：行内控件聚焦与快捷键导航 */

import { isTabbableVisible } from '@/utils/focus-tab-common'

export function pickRemindTimerStopFocusEl(node) {
  if (!node) return null
  if (node.classList && node.classList.contains('el-switch')) {
    const input = node.querySelector('.el-switch__input')
    return input && isTabbableVisible(input) ? input : null
  }
  if (node.matches && node.matches('button, .el-button')) {
    return isTabbableVisible(node) ? node : null
  }
  const input = node.querySelector && node.querySelector('input:not([type="hidden"])')
  if (input && isTabbableVisible(input)) return input
  if (node.matches && node.matches('input:not([type="hidden"])') && isTabbableVisible(node)) {
    return node
  }
  return null
}

export function getRemindTimerRowStops(tr) {
  if (!tr || !tr.querySelectorAll) return []
  const stops = []
  tr.querySelectorAll('[data-remind-stop]').forEach((node) => {
    const el = pickRemindTimerStopFocusEl(node)
    if (el) stops.push(el)
  })
  return stops
}

export function resolveRemindTimerRowIndex(tableEl, activeEl) {
  if (!tableEl || !activeEl || !activeEl.closest) return -1
  const tr = activeEl.closest('.el-table__body tbody tr')
  if (!tr || !tableEl.contains(tr)) return -1
  const rows = tableEl.querySelectorAll('.el-table__body tbody tr')
  return Array.prototype.indexOf.call(rows, tr)
}

export function resolveRemindTimerColIndex(stops, activeEl) {
  if (!stops.length || !activeEl) return 0
  const direct = stops.indexOf(activeEl)
  if (direct >= 0) return direct
  for (let i = 0; i < stops.length; i++) {
    if (stops[i].contains && stops[i].contains(activeEl)) return i
  }
  if (activeEl.closest) {
    for (let i = 0; i < stops.length; i++) {
      const sw = stops[i].closest && stops[i].closest('.el-switch')
      if (sw && activeEl.closest('.el-switch') === sw) return i
    }
  }
  return 0
}

/** 名称等可编辑文本框内保留 Del 删字，不删行 */
export function shouldRemindTimerDeleteRow(activeEl) {
  if (!activeEl || activeEl.tagName !== 'INPUT') return true
  if (activeEl.readOnly) return true
  const type = (activeEl.type || 'text').toLowerCase()
  if (type === 'checkbox' || type === 'radio') return true
  if (type === 'text' || type === 'number' || type === 'search') {
    const editable = activeEl.closest && activeEl.closest('[data-remind-stop="label"]')
    if (editable) return false
    const inputNumber = activeEl.closest && activeEl.closest('.el-input-number')
    if (inputNumber) return false
  }
  return true
}
