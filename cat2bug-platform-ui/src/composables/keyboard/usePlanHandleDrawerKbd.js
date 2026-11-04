import { getCurrentInstance } from 'vue'
import { getLogicalTabStopsInScope } from '@/utils/focus-tab-common'

export function usePlanHandleDrawerKbd() {
  const instance = getCurrentInstance()
  const vm = () => instance && instance.proxy

  // 仅闭包内使用，勿放入 setup 返回值（Vue 禁止 setup 暴露 $/_ 前缀属性）
  const focusHandlePlanFirstControl = () => {
    const host = vm()
    if (!host) return
    const list = host.$refs && host.$refs.list
    const root = (list && list.$el) || host.$el
    if (!root) return
    const first = getLogicalTabStopsInScope(root)[0]
    if (!first) return
    if (typeof first.focus === 'function') {
      try {
        first.focus({ preventScroll: false })
      } catch (err) {
        first.focus()
      }
    }
  }

  const onHandlePlanDrawerOpenedKbd = () => {
    const host = vm()
    if (!host || typeof host.$nextTick !== 'function') return
    host.$nextTick(() => {
      requestAnimationFrame(() => {
        focusHandlePlanFirstControl()
      })
    })
  }

  const shortcutClose = (e) => {
    if (e) {
      e.preventDefault()
      e.stopPropagation()
      if (typeof e.stopImmediatePropagation === 'function') e.stopImmediatePropagation()
    }
    const host = vm()
    if (host && typeof host.requestClosePlanHandleDrawer === 'function') {
      host.requestClosePlanHandleDrawer()
    }
  }

  const shortcutSave = () => {
    const host = vm()
    if (host && typeof host.submitForm === 'function') {
      host.submitForm()
    }
  }

  return {
    onHandlePlanDrawerOpenedKbd,
    shortcutClose,
    shortcutSave
  }
}
