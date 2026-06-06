/**
 * 全局 Tab 方向：按住方向键再按 Tab 控制焦点切换方向。
 *   - 默认 Tab / 按住 ↓ 再 Tab → 正向（下一个可聚焦元素）
 *   - 按住 ↑ 再 Tab → 反向（上一个可聚焦元素）
 *
 * 在捕获阶段处理，优先于各组件内 Tab 逻辑；命令面板打开时不介入。
 * input/textarea 上方向键松开后仍可在短时间内用 Tab 延续方向（避免 keyup 早于 Tab）。
 */

import { shortcutService } from '@/plugins/shortcut/service'
import { dismissComboPopoverIfLeaving, focusNextFromComboShell } from '@/utils/combo-focus-tab'
import { focusNextFromUploadShell } from '@/utils/upload-focus-tab'
import {
  getTabScopeRoot,
  resolveLogicalTabStopIndex,
  getLogicalTabStopsInScope
} from '@/utils/focus-tab-common'
import {
  markArrowTabDirection,
  releaseArrowKey,
  hasArrowTabIntent,
  getArrowTabDirection,
  clearArrowTabIntentAfterTab,
  cancelPendingArrowTabIntent,
  resetArrowTabIntent
} from '@/utils/tab-direction-intent'

function isEditableElement(el) {
  if (!el || el === document.body) return false
  const tag = el.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return true
  if (el.isContentEditable) return true
  return false
}

function focusSibling(direction) {
  const active = document.activeElement
  const reverse = direction < 0

  const uploadShell = active && active.closest && active.closest('.cat2bug-upload-focus-target')
  if (uploadShell) {
    return focusNextFromUploadShell(uploadShell, reverse)
  }

  const comboShell = active && active.closest && active.closest('.cat2bug-combo-focus-target')
  if (comboShell) {
    return focusNextFromComboShell(comboShell, reverse)
  }

  const scopeRoot = getTabScopeRoot(active)
  const list = getLogicalTabStopsInScope(scopeRoot)
  if (!list.length) return false
  let idx = resolveLogicalTabStopIndex(list, active)
  if (idx < 0) {
    idx = direction < 0 ? list.length : -1
  }
  const next = idx + direction
  if (next < 0 || next >= list.length) return false
  const target = list[next]
  dismissComboPopoverIfLeaving(active, target)
  try {
    target.focus({ preventScroll: false })
  } catch (err) {
    if (typeof target.focus === 'function') target.focus()
  }
  return true
}

function onKeyDown(e) {
  if (e.isComposing) return
  if (shortcutService.palette && shortcutService.palette.open) return

  if (e.key === 'ArrowUp') {
    markArrowTabDirection(-1)
    return
  }
  if (e.key === 'ArrowDown') {
    markArrowTabDirection(1)
    return
  }

  // 在输入框内输入内容时取消「待定的方向 + Tab」
  if (hasArrowTabIntent() && isEditableElement(document.activeElement)) {
    const k = e.key
    const isTyping =
      (k && k.length === 1 && !e.metaKey && !e.ctrlKey && !e.altKey) ||
      k === 'Backspace' ||
      k === 'Delete' ||
      k === 'Enter'
    if (isTyping && k !== 'Tab') {
      cancelPendingArrowTabIntent()
    }
  }

  if (e.key !== 'Tab' || e.altKey) return

  if (!hasArrowTabIntent()) {
    return
  }

  // 按住 Cmd/Ctrl 显示字段提示时仍允许 ↑/↓ + Tab 切字段
  const dir = getArrowTabDirection()
  if (dir === 0) return

  if (e.shiftKey) return

  if (!focusSibling(dir)) return

  e.preventDefault()
  e.stopPropagation()
  clearArrowTabIntentAfterTab()
}

function onKeyUp(e) {
  releaseArrowKey(e.key)
}

const TabDirectionPlugin = {
  install() {
    document.addEventListener('keydown', onKeyDown, true)
    document.addEventListener('keyup', onKeyUp, true)
    window.addEventListener('blur', resetArrowTabIntent)
  }
}

export default TabDirectionPlugin
