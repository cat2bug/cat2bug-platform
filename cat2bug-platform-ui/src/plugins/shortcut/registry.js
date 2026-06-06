/**
 * 页面级动作注册表。页面在 mounted/activated 时注册其动作面板内容，
 * 在 beforeDestroy/deactivated 时注销，避免跨页误触发。
 */
import Vue from 'vue'

const state = Vue.observable({
  // scopeKey -> { actions: [{ key, titleKey?, title?, defaultLetter, run }] }
  pages: {}
})

export const pageRegistry = {
  state,

  registerPage(scopeKey, actions) {
    Vue.set(state.pages, scopeKey, { actions: actions || [] })
  },

  unregisterPage(scopeKey) {
    Vue.delete(state.pages, scopeKey)
  },

  /** 当前已注册的所有页面动作（合并）。多数情况下同时只有一个页面注册。 */
  getActiveActions() {
    const result = []
    Object.keys(state.pages).forEach((scopeKey) => {
      const page = state.pages[scopeKey]
      if (page && Array.isArray(page.actions)) {
        page.actions.forEach((a) => result.push({ ...a, scopeKey }))
      }
    })
    return result
  },

  hasActions() {
    return Object.keys(state.pages).some(
      (k) => state.pages[k] && state.pages[k].actions && state.pages[k].actions.length > 0
    )
  }
}
