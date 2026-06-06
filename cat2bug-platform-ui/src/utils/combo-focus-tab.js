/**
 * 组合选择器 Tab 顺序：外框单一停靠点，内部 input / 按钮不参与 Tab 序列。
 * 用于 SelectProjectMember、SelectModule 等。
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

const COMBO_INNER_PATCH_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '.el-tag__close',
  '[tabindex]:not([tabindex="-1"])'
].join(',')

const COMBO_SHELL_SELECTOR = '.cat2bug-combo-focus-target'

function isMemberSearchInput(el, shell) {
  if (!el || !shell || !shell.classList.contains('select-project-member-input')) return false
  const searchRoot = shell.querySelector('.select-project-member-search-input')
  if (!searchRoot) return false
  return searchRoot === el || searchRoot.contains(el)
}

/** 将组合选择器内部可聚焦元素移出 Tab 序列，仅保留外框 */
export function patchComboFocusTarget(shell) {
  if (!shell || !shell.classList || !shell.classList.contains('cat2bug-combo-focus-target')) return
  if (shell.getAttribute('tabindex') !== '0') {
    shell.setAttribute('tabindex', '0')
  }
  shell.querySelectorAll(COMBO_INNER_PATCH_SELECTOR).forEach((el) => {
    if (el !== shell && shell.contains(el) && el.getAttribute('tabindex') !== '-1') {
      el.setAttribute('tabindex', '-1')
    }
  })
}

export function patchAllComboFocusTargets(root = document) {
  if (!root || !root.querySelectorAll) return
  root.querySelectorAll(COMBO_SHELL_SELECTOR).forEach(patchComboFocusTarget)
}

export function observeComboFocusTarget(shell) {
  patchComboFocusTarget(shell)
  if (shell.__cat2bugComboTabObs) return
  shell.__cat2bugComboTabObs = true
  const obs = new MutationObserver(() => patchComboFocusTarget(shell))
  obs.observe(shell, {
    childList: true,
    subtree: true,
    attributes: true,
    attributeFilter: ['tabindex', 'disabled', 'class']
  })
}

function getComboShellFromActive() {
  const active = document.activeElement
  if (!active || !active.closest) return null
  return active.closest(COMBO_SHELL_SELECTOR)
}

function invokeTabAway(shell) {
  if (typeof shell.__cat2bugOnTabAway === 'function') {
    shell.__cat2bugOnTabAway()
  }
}

/** 焦点即将离开组合选择器外框时关闭下拉（↑/↓+Tab、快捷键跳字段等） */
export function dismissComboPopoverIfLeaving(currentActive, nextTarget) {
  if (!currentActive || !currentActive.closest) return
  const shell = currentActive.closest(COMBO_SHELL_SELECTOR)
  if (!shell) return
  if (nextTarget && (shell === nextTarget || shell.contains(nextTarget))) return
  invokeTabAway(shell)
}

/** Tab / Shift+Tab：按表单项顺序一次离开到相邻字段 */
export function focusNextFromComboShell(shell, reverse = false) {
  if (!shell) return false
  patchComboFocusTarget(shell)
  invokeTabAway(shell)
  const scopeRoot = getTabScopeRoot(shell)
  const all = getLogicalTabStopsInScope(scopeRoot)
  if (!all.length || resolveLogicalTabStopIndex(all, shell) < 0) return false
  return !!focusAdjacentLogicalTabStop(scopeRoot, shell, reverse)
}

export function onComboTabKeydown(e) {
  if (e.key !== 'Tab' || e.metaKey || e.ctrlKey || e.altKey) return
  if (e.isComposing) return
  const shell = getComboShellFromActive()
  if (!shell) return
  const reverse = resolveTabReverseFromEvent(e)
  const hadArrowIntent = hasArrowTabIntent() && getArrowTabDirection() !== 0 && !e.shiftKey
  e.preventDefault()
  e.stopPropagation()
  focusNextFromComboShell(shell, reverse)
  if (hadArrowIntent) clearArrowTabIntentAfterTab()
}

export function onComboFocusIn(e) {
  const t = e.target
  if (!t || !t.closest) return
  const shell = t.closest(COMBO_SHELL_SELECTOR)
  if (!shell || t === shell || !shell.contains(t)) return
  patchComboFocusTarget(shell)
  if (isMemberSearchInput(t, shell)) return
  if (shell.__cat2bugComboRefocusing) return
  shell.__cat2bugComboRefocusing = true
  try {
    shell.focus()
  } finally {
    requestAnimationFrame(() => {
      shell.__cat2bugComboRefocusing = false
    })
  }
}
