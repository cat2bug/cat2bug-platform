/**
 * Tab 顺序公共工具：可聚焦元素查询与可见性判断。
 */

export const TABBABLE_SELECTOR = [
  'a[href]:not([tabindex="-1"])',
  'button:not([disabled]):not([tabindex="-1"])',
  'input:not([disabled]):not([type="hidden"]):not([tabindex="-1"])',
  'select:not([disabled]):not([tabindex="-1"])',
  'textarea:not([disabled]):not([tabindex="-1"])',
  '[contenteditable="true"]:not([tabindex="-1"])',
  '[tabindex]:not([tabindex="-1"])'
].join(',')

/** Element UI 开关内部 checkbox 宽高为 0，以 .el-switch 容器判断可见性 */
export function isSwitchTabbable(sw, input = null) {
  if (!sw || !sw.getBoundingClientRect) return false
  const inp = input || sw.querySelector('.el-switch__input')
  if (!inp || inp.disabled) return false
  if (sw.closest('[aria-hidden="true"]')) return false
  const style = window.getComputedStyle(sw)
  if (style.visibility === 'hidden' || style.display === 'none') return false
  const rect = sw.getBoundingClientRect()
  return rect.width > 0 || rect.height > 0
}

/** 是否参与 Tab 序列（Element UI 开关 input 宽高为 0，以 .el-switch 容器为准） */
export function isTabbableVisible(el) {
  if (!el || !el.getBoundingClientRect) return false
  if (el.closest('[aria-hidden="true"]')) return false

  const sw = el.closest && el.closest('.el-switch')
  if (sw && (el.classList?.contains('el-switch__input') || el.type === 'checkbox')) {
    return isSwitchTabbable(sw, el)
  }

  const style = window.getComputedStyle(el)
  if (style.visibility === 'hidden' || style.display === 'none') return false
  const rect = el.getBoundingClientRect()
  return rect.width > 0 || rect.height > 0
}

export function getTabbablesInScope(root, selector = TABBABLE_SELECTOR) {
  if (!root || !root.querySelectorAll) return []
  return Array.from(root.querySelectorAll(selector)).filter(isTabbableVisible)
}

/** 单个表单项的逻辑 Tab 停靠点（与动态字段顺序一致） */
export function pickTabStopForFormItem(item) {
  if (!item || !item.querySelector) return null
  const content = item.querySelector('.el-form-item__content') || item

  const combo = content.querySelector('.cat2bug-combo-focus-target')
  if (combo && isTabbableVisible(combo)) return combo

  const upload = content.querySelector('.cat2bug-upload-focus-target')
  if (upload && isTabbableVisible(upload)) return upload

  const sw = content.querySelector('.el-switch')
  if (sw) {
    const input = sw.querySelector('.el-switch__input')
    if (isSwitchTabbable(sw, input)) return input
  }

  const tabbables = Array.from(content.querySelectorAll(TABBABLE_SELECTOR)).filter(isTabbableVisible)
  return tabbables[0] || null
}

/**
 * 按 .el-form-item DOM 顺序收集逻辑 Tab 停靠点（动态配置字段顺序与 Tab 一致）。
 */
export function getLogicalTabStopsInScope(root) {
  if (!root || !root.querySelectorAll) return []
  const form = root.querySelector('form') || root
  const stops = []
  form.querySelectorAll('.el-form-item').forEach((item) => {
    const stop = pickTabStopForFormItem(item)
    if (stop) stops.push(stop)
  })
  return stops
}

/** 将 document.activeElement 映射到逻辑停靠点列表中的索引 */
export function resolveLogicalTabStopIndex(list, active) {
  if (!active || !list.length) return -1
  const direct = list.indexOf(active)
  if (direct >= 0) return direct
  for (let i = 0; i < list.length; i++) {
    if (list[i].contains(active)) return i
  }
  if (active.closest) {
    const combo = active.closest('.cat2bug-combo-focus-target')
    if (combo) {
      const idx = list.indexOf(combo)
      if (idx >= 0) return idx
    }
    const upload = active.closest('.cat2bug-upload-focus-target')
    if (upload) {
      const idx = list.indexOf(upload)
      if (idx >= 0) return idx
    }
    const sw = active.closest('.el-switch')
    if (sw) {
      const input = sw.querySelector('.el-switch__input')
      if (input) {
        const idx = list.indexOf(input)
        if (idx >= 0) return idx
      }
    }
  }
  return -1
}

export function focusAdjacentLogicalTabStop(scopeRoot, currentEl, reverse = false) {
  const all = getLogicalTabStopsInScope(scopeRoot)
  if (!all.length) return false
  let idx = resolveLogicalTabStopIndex(all, currentEl)
  if (idx < 0) {
    idx = reverse ? all.length : -1
  }
  const nextIdx = idx + (reverse ? -1 : 1)
  if (nextIdx < 0 || nextIdx >= all.length) return false
  const target = all[nextIdx]
  try {
    target.focus({ preventScroll: false })
  } catch (err) {
    if (typeof target.focus === 'function') target.focus()
  }
  return target
}

export function getTabScopeRoot(shell) {
  if (shell) {
    const modal = shell.closest('.el-dialog__wrapper, .el-drawer__wrapper')
    if (modal) {
      return modal.querySelector('.el-dialog, .el-drawer') || modal
    }
  }
  const active = document.activeElement
  if (active) {
    const modal = active.closest('.el-dialog__wrapper, .el-drawer__wrapper')
    if (modal) {
      return modal.querySelector('.el-dialog, .el-drawer') || modal
    }
  }
  return document
}
