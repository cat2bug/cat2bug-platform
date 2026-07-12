/**
 * 页面级动作注册表。页面在 mounted/activated 时注册其动作面板内容，
 * 在 beforeUnmount/deactivated 时注销，避免跨页误触发。
 */
import { reactive } from 'vue'

const state = reactive({
  pages: {}
})

export const pageRegistry = {
  state,

  registerPage(scopeKey, actions) {
    state.pages[scopeKey] = { actions: actions || [] }
  },

  unregisterPage(scopeKey) {
    delete state.pages[scopeKey]
  },

  getActiveActions() {
    const result = []
    Object.keys(state.pages).forEach((scopeKey) => {
      const page = state.pages[scopeKey]
      if (page && Array.isArray(page.actions)) {
        page.actions.forEach((a) => result.push({ ...a, scopeKey }))
      }
    })
    return result
  }
}
