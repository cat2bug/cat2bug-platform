/**
 * Element UI 开关键盘与 Tab 顺序修复：
 *   - 仅保留内部 checkbox 一个 Tab 停靠点（移除根节点 tabindex，避免双 Tab）
 *   - 聚焦后：← 关、→ 开（空格由原生 checkbox 切换）
 * 在捕获阶段处理，命令面板打开时不介入。
 */

import { shortcutService } from '@/plugins/shortcut/service'

function getSwitchRoot(el) {
  if (!el) return null
  if (el.classList && el.classList.contains('el-switch')) return el
  return el.closest('.el-switch')
}

/** 保证开关只有一个 Tab 停靠点：仅 .el-switch__input，根节点不参与 Tab 序列 */
function patchSwitchTabOrder(root) {
  if (!root || !root.classList || !root.classList.contains('el-switch')) return
  const input = root.querySelector('.el-switch__input')
  if (input) {
    if (input.getAttribute('tabindex') === '-1') {
      input.removeAttribute('tabindex')
    }
  }
  if (root.hasAttribute('tabindex')) {
    root.removeAttribute('tabindex')
  }
}

function patchAllSwitches(node = document) {
  if (!node || !node.querySelectorAll) return
  node.querySelectorAll('.el-switch').forEach(patchSwitchTabOrder)
}

function observeSwitch(root) {
  patchSwitchTabOrder(root)
  if (root.__cat2bugSwitchObs) return
  root.__cat2bugSwitchObs = true
  const obs = new MutationObserver(() => patchSwitchTabOrder(root))
  obs.observe(root, {
    childList: true,
    subtree: true,
    attributes: true,
    attributeFilter: ['tabindex']
  })
}

function getFocusedSwitchVm() {
  const root = getSwitchRoot(document.activeElement)
  if (!root) return null
  const vm = root.__vue__
  if (!vm || vm.$options.name !== 'ElSwitch') return null
  if (vm.switchDisabled) return null
  return vm
}

function onFocusIn(e) {
  const root = getSwitchRoot(e.target)
  if (root) patchSwitchTabOrder(root)
}

function onKeyDown(e) {
  if (e.isComposing) return
  if (e.metaKey || e.ctrlKey || e.altKey) return
  if (shortcutService.palette && shortcutService.palette.open) return

  const vm = getFocusedSwitchVm()
  if (!vm) return

  const active = document.activeElement
  const onInput = active && active.classList && active.classList.contains('el-switch__input')
  if (!onInput) return

  if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight') return

  const next = e.key === 'ArrowRight' ? vm.activeValue : vm.inactiveValue
  e.preventDefault()
  e.stopPropagation()
  if (vm.value === next) return
  vm.$emit('input', next)
  vm.$emit('change', next)
}

const SwitchKeyboardPlugin = {
  install() {
    patchAllSwitches()
    document.querySelectorAll('.el-switch').forEach(observeSwitch)

    const observer = new MutationObserver((mutations) => {
      mutations.forEach((m) => {
        m.addedNodes.forEach((node) => {
          if (node.nodeType !== 1) return
          if (node.classList && node.classList.contains('el-switch')) {
            observeSwitch(node)
          }
          if (node.querySelectorAll) {
            node.querySelectorAll('.el-switch').forEach(observeSwitch)
          }
        })
      })
    })
    if (document.body) {
      observer.observe(document.body, { childList: true, subtree: true })
    }
    document.addEventListener('focusin', onFocusIn, true)
    document.addEventListener('keydown', onKeyDown, true)
  }
}

export default SwitchKeyboardPlugin
