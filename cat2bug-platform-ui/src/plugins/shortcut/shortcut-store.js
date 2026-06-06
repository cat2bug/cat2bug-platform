/**
 * 快捷键配置存储：仅持久化用户改动的差异项到 localStorage。
 * 默认映射来自代码（keymap.js + 运行时菜单派生）。
 */
import Vue from 'vue'
import { STORAGE_KEY, DEFAULT_LEADERS, normalizeKey } from './keymap'

function load() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    return JSON.parse(raw)
  } catch (e) {
    return null
  }
}

const persisted = load() || {}

const state = Vue.observable({
  enabled: persisted.enabled !== false,
  leaders: {
    nav: persisted.leaders && persisted.leaders.nav ? persisted.leaders.nav : DEFAULT_LEADERS.nav,
    action: persisted.leaders && persisted.leaders.action ? persisted.leaders.action : DEFAULT_LEADERS.action
  },
  // bindingId -> 用户自定义字母（仅差异项）
  overrides: { ...(persisted.overrides || {}) }
})

function persist() {
  try {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        enabled: state.enabled,
        leaders: state.leaders,
        overrides: state.overrides
      })
    )
  } catch (e) {
    /* ignore quota / 隐私模式 */
  }
}

export const shortcutStore = {
  state,

  isEnabled() {
    return state.enabled
  },
  setEnabled(val) {
    state.enabled = !!val
    persist()
  },

  getLeader(kind) {
    return state.leaders[kind] || DEFAULT_LEADERS[kind]
  },

  /** 取某绑定的字母：用户覆盖优先，否则用默认 */
  getLetter(bindingId, defaultLetter) {
    const ov = state.overrides[bindingId]
    if (ov != null && ov !== '') return normalizeKey(ov)
    return normalizeKey(defaultLetter)
  },

  /** 设置用户覆盖；传入与默认相同的值或空则视为清除覆盖 */
  setOverride(bindingId, letter, defaultLetter) {
    const norm = normalizeKey(letter)
    if (!norm || norm === normalizeKey(defaultLetter)) {
      Vue.delete(state.overrides, bindingId)
    } else {
      Vue.set(state.overrides, bindingId, norm)
    }
    persist()
  },

  resetBinding(bindingId) {
    Vue.delete(state.overrides, bindingId)
    persist()
  },

  resetAll() {
    state.overrides = {}
    state.leaders = { ...DEFAULT_LEADERS }
    persist()
  }
}
