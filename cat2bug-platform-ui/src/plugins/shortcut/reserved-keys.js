/**
 * 单字母保留键：按作用域统一维护，供页面动作、表单字段、行分配与设置页校验共用。
 * 字母均为大写单字符（与 normalizeKey 一致）。
 */

/** 浏览器 ⌘/Ctrl+[ ] 后退/前进，禁止用于快捷键绑定与日期面板导航 */
export const BROWSER_RESERVED_SYMBOLS = new Set(['[', ']'])

export const PAGE_ACTION_RESERVED = new Set(['A', 'C', 'M', 'N', 'Q', 'T', 'V', 'X', 'Z'])

/** 页面保留 + 浏览器/OS 窗口、地址栏、刷新相关 */
export const FIELD_HINT_BLOCKED = new Set([
  ...PAGE_ACTION_RESERVED,
  'W', 'R', 'L', 'H'
])

/** 与页面动作保留一致，用于表格行动态分配 */
export const ROW_KBD_RESERVED = PAGE_ACTION_RESERVED

/** 键盘设置页用户不可绑定的字母并集 */
export const SETTINGS_NEVER_BIND = new Set([
  ...PAGE_ACTION_RESERVED,
  'W', 'R', 'L', 'H'
])

/**
 * @param {string} key
 * @returns {boolean}
 */
export function isReservedSymbol(key) {
  if (!key) return false
  return BROWSER_RESERVED_SYMBOLS.has(String(key).trim())
}

/**
 * @param {string} letter
 * @param {'page'|'field'|'settings'|'row'} scope
 * @returns {boolean}
 */
export function isReservedForScope(letter, scope) {
  if (isReservedSymbol(letter)) return true
  if (!letter) return false
  const norm = String(letter).trim().toUpperCase()
  if (!norm || norm.length !== 1) return false
  switch (scope) {
    case 'page':
    case 'row':
      return PAGE_ACTION_RESERVED.has(norm)
    case 'field':
      return FIELD_HINT_BLOCKED.has(norm)
    case 'settings':
      return SETTINGS_NEVER_BIND.has(norm)
    default:
      return false
  }
}
