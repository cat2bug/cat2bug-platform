/**
 * 快捷键配置存储：仅持久化用户改动的差异项到 localStorage。
 * 默认映射来自代码（keymap.js + 运行时菜单派生）。
 */
import Vue from 'vue'
import { STORAGE_KEY, DEFAULT_LEADERS, normalizeKey } from './keymap'
import { isReservedForScope, isReservedSymbol } from './reserved-keys'

/** @param {string} letter @param {'page'|'field'|'settings'|'row'} [scope='settings'] */
export function isBindingLetterAllowed(letter, scope = 'settings') {
  const norm = normalizeKey(letter)
  if (!norm) return true
  return !isReservedForScope(norm, scope)
}

export function validateOverride(bindingId, letter, defaultLetter) {
  if (isReservedSymbol(letter)) {
    return { ok: false, reason: 'reserved' }
  }
  const norm = normalizeKey(letter)
  if (!norm || norm === normalizeKey(defaultLetter)) {
    return { ok: true }
  }
  if (isReservedForScope(norm, 'settings')) {
    return { ok: false, reason: 'reserved' }
  }
  return { ok: true }
}

function load() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const data = JSON.parse(raw)
    if (data.overrides) {
      const legacyView = normalizeKey(data.overrides['action.defect.switchView'])
      if (legacyView === 'T' || legacyView === 'M' || legacyView === 'L') {
        delete data.overrides['action.defect.switchView']
        try {
          localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
        } catch (e) { /* ignore */ }
      }
      const legacyQuery = normalizeKey(data.overrides['action.defect.query'])
      if (legacyQuery === 'Q') {
        delete data.overrides['action.defect.query']
        try {
          localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
        } catch (e) { /* ignore */ }
      }
      const legacyAddMenu = normalizeKey(data.overrides['action.defect.addMenu'])
      if (legacyAddMenu === 'N') {
        delete data.overrides['action.defect.addMenu']
        try {
          localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
        } catch (e) { /* ignore */ }
      }
      const legacyStatistic = normalizeKey(data.overrides['action.defect.statistic'])
      if (legacyStatistic === 'V') {
        delete data.overrides['action.defect.statistic']
        try {
          localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
        } catch (e) { /* ignore */ }
      }
      if (data.overrides['action.defect.statisticPanel'] != null) {
        if (data.overrides['action.defect.statistic'] == null) {
          data.overrides['action.defect.statistic'] = data.overrides['action.defect.statisticPanel']
        }
        delete data.overrides['action.defect.statisticPanel']
        try {
          localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
        } catch (e) { /* ignore */ }
      }
      if (data.overrides['action.statistic-template.back'] != null) {
        delete data.overrides['action.statistic-template.back']
        try {
          localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
        } catch (e) { /* ignore */ }
      }
      // 项目文档：W → F，规避 ⇧⌘W 浏览器保留键
      const legacyDoc = normalizeKey(data.overrides['nav.menu.project.document'])
      if (legacyDoc === 'W') {
        delete data.overrides['nav.menu.project.document']
        try {
          localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
        } catch (e) { /* ignore */ }
      }
      const legacyToggle = normalizeKey(data.overrides['nav.layout.sidebarToggle'])
      if (legacyToggle === '`') {
        delete data.overrides['nav.layout.sidebarToggle']
        try {
          localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
        } catch (e) { /* ignore */ }
      }
    }
    return data
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
    const validation = validateOverride(bindingId, letter, defaultLetter)
    if (!validation.ok) return validation
    const norm = normalizeKey(letter)
    if (!norm || norm === normalizeKey(defaultLetter)) {
      Vue.delete(state.overrides, bindingId)
    } else {
      Vue.set(state.overrides, bindingId, norm)
    }
    persist()
    return { ok: true }
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
