/**
 * 全局修补组合选择器（成员 / 交付物）的 Tab 顺序（外框单停靠点）。
 */

import {
  observeComboFocusTarget,
  onComboFocusIn,
  onComboTabKeydown,
  patchAllComboFocusTargets
} from '@/utils/combo-focus-tab'

const ComboFocusTabPlugin = {
  install() {
    patchAllComboFocusTargets()
    document.querySelectorAll('.cat2bug-combo-focus-target').forEach(observeComboFocusTarget)

    const observer = new MutationObserver((mutations) => {
      mutations.forEach((m) => {
        m.addedNodes.forEach((node) => {
          if (node.nodeType !== 1) return
          if (node.classList && node.classList.contains('cat2bug-combo-focus-target')) {
            observeComboFocusTarget(node)
          }
          if (node.querySelectorAll) {
            node.querySelectorAll('.cat2bug-combo-focus-target').forEach(observeComboFocusTarget)
          }
        })
      })
    })
    if (document.body) {
      observer.observe(document.body, { childList: true, subtree: true })
    }
    document.addEventListener('focusin', onComboFocusIn, true)
    document.addEventListener('keydown', onComboTabKeydown, true)
  }
}

export default ComboFocusTabPlugin
