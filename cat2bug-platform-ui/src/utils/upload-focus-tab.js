/**
 * 上传组件 Tab 顺序：外框单一停靠点，内部按钮 / el-upload / 图片卡片不参与 Tab 序列。
 */

import {
  focusAdjacentLogicalTabStop,
  getTabScopeRoot,
  resolveLogicalTabStopIndex,
  getLogicalTabStopsInScope
} from '@/utils/focus-tab-common'
import {
  getArrowTabDirection,
  hasArrowTabIntent,
  clearArrowTabIntentAfterTab,
  resolveTabReverseFromEvent
} from '@/utils/tab-direction-intent'
import {
  beginNativeFilePickerSession,
  isNativeFilePickerOpen
} from '@/utils/native-file-picker'

/** 上传区内需要移出 Tab 序列的元素（含尚未打 tabindex 的原生控件） */
const UPLOAD_INNER_PATCH_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '.el-upload',
  '.el-upload-list__item',
  '[tabindex]:not([tabindex="-1"])'
].join(',')

/** 将上传区内部所有可聚焦元素移出 Tab 序列，仅保留外框 */
export function patchUploadFocusTarget(shell) {
  if (!shell || !shell.classList || !shell.classList.contains('cat2bug-upload-focus-target')) return
  if (shell.getAttribute('tabindex') !== '0') {
    shell.setAttribute('tabindex', '0')
  }
  shell.querySelectorAll(UPLOAD_INNER_PATCH_SELECTOR).forEach((el) => {
    if (el !== shell && shell.contains(el) && el.getAttribute('tabindex') !== '-1') {
      el.setAttribute('tabindex', '-1')
    }
  })
}

export function patchAllUploadFocusTargets(root = document) {
  if (!root || !root.querySelectorAll) return
  root.querySelectorAll('.cat2bug-upload-focus-target').forEach(patchUploadFocusTarget)
}

export function observeUploadFocusTarget(shell) {
  patchUploadFocusTarget(shell)
  if (shell.__cat2bugUploadTabObs) return
  shell.__cat2bugUploadTabObs = true
  const obs = new MutationObserver(() => patchUploadFocusTarget(shell))
  obs.observe(shell, {
    childList: true,
    subtree: true,
    attributes: true,
    attributeFilter: ['tabindex', 'disabled', 'class']
  })
}

function getUploadShellFromActive() {
  const active = document.activeElement
  if (!active || !active.closest) return null
  return active.closest('.cat2bug-upload-focus-target')
}

/** Tab / Shift+Tab：按表单项顺序一次离开到相邻字段 */
export function focusNextFromUploadShell(shell, reverse = false) {
  if (!shell) return false
  patchUploadFocusTarget(shell)
  const scopeRoot = getTabScopeRoot(shell)
  const all = getLogicalTabStopsInScope(scopeRoot)
  if (!all.length || resolveLogicalTabStopIndex(all, shell) < 0) return false
  return !!focusAdjacentLogicalTabStop(scopeRoot, shell, reverse)
}

export function onUploadTabKeydown(e) {
  if (e.key !== 'Tab' || e.metaKey || e.ctrlKey || e.altKey) return
  if (e.isComposing) return
  const shell = getUploadShellFromActive()
  if (!shell) return
  const reverse = resolveTabReverseFromEvent(e)
  const hadArrowIntent = hasArrowTabIntent() && getArrowTabDirection() !== 0 && !e.shiftKey
  e.preventDefault()
  e.stopPropagation()
  focusNextFromUploadShell(shell, reverse)
  if (hadArrowIntent) clearArrowTabIntentAfterTab()
}

export function onUploadFocusIn(e) {
  if (isNativeFilePickerOpen()) return
  const t = e.target
  if (!t || !t.closest) return
  if (t.matches && t.matches('input[type="file"]')) return
  const shell = t.closest('.cat2bug-upload-focus-target')
  if (!shell || t === shell) return
  if (!shell.contains(t)) return
  patchUploadFocusTarget(shell)
  if (shell.__cat2bugUploadRefocusing) return
  shell.__cat2bugUploadRefocusing = true
  try {
    shell.focus()
  } finally {
    requestAnimationFrame(() => {
      shell.__cat2bugUploadRefocusing = false
    })
  }
}

/** 点击上传区内的选文件按钮 / file input 时标记原生文件框会话，避免 focusin 抢焦点 */
export function onUploadAreaClickCapture(e) {
  const t = e.target
  if (!t || !t.closest) return
  const shell = t.closest('.cat2bug-upload-focus-target')
  if (!shell) return
  const onFileInput = t.matches && t.matches('input[type="file"]')
  const onUploadTrigger =
    !!t.closest('.el-upload') &&
    !!(t.closest('button') || t.closest('.update-button') || t.closest('.el-upload-dragger'))
  if (!onFileInput && !onUploadTrigger) return
  beginNativeFilePickerSession()
  const active = document.activeElement
  if (active && typeof active.blur === 'function' && shell.contains(active)) {
    active.blur()
  }
}
