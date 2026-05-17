/**
 * 成员文字头像：首字/首字母 + 固定色板白字（不依赖拼音库）。
 */

/** 主流 SaaS 文字头像色板（白字对比度） */
export const AVATAR_PALETTE = [
  '#1890ff',
  '#52c41a',
  '#13c2c2',
  '#722ed1',
  '#eb2f96',
  '#fa8c16',
  '#f5222d',
  '#2f54eb',
  '#597ef7',
  '#a0d911'
]

const AVATAR_TEXT_COLOR = '#FFFFFF'
const AVATAR_FONT_WEIGHT = 700

/** @param {string} name */
export function getAvatarInitial(name) {
  if (name == null || typeof name !== 'string') return '?'
  const trimmed = name.trim()
  if (!trimmed) return '?'
  const char = [...trimmed][0]
  if (/[a-zA-Z]/.test(char)) return char.toUpperCase()
  return char
}

/** 参与着色哈希的稳定 key（首字符，拉丁字母统一小写） */
export function getAvatarColorKey(name) {
  if (name == null || typeof name !== 'string') return '?'
  const trimmed = name.trim()
  if (!trimmed) return '?'
  const char = [...trimmed][0]
  if (/[a-zA-Z]/.test(char)) return char.toLowerCase()
  return char
}

function hashString(str) {
  let h = 0
  for (let i = 0; i < str.length; i++) {
    h = (h << 5) - h + str.charCodeAt(i)
    h |= 0
  }
  return Math.abs(h)
}

/**
 * @param {string} colorKey
 * @returns {{ backgroundColor: string, color: string, fontWeight: number }}
 */
export function getAvatarPaletteStyle(colorKey) {
  const key = colorKey || '?'
  const index = hashString(key) % AVATAR_PALETTE.length
  return {
    backgroundColor: AVATAR_PALETTE[index],
    color: AVATAR_TEXT_COLOR,
    fontWeight: AVATAR_FONT_WEIGHT
  }
}

/** @param {string | null | undefined} value */
export function isNonEmptyAvatarPath(value) {
  if (value == null || typeof value !== 'string') return false
  const trimmed = value.trim()
  return !!trimmed && trimmed !== 'null' && trimmed !== 'undefined'
}

/** @param {{ avatar?: string, avatarUrl?: string }} member */
export function hasMemberPhoto(member) {
  if (!member) return false
  return isNonEmptyAvatarPath(member.avatarUrl) || isNonEmptyAvatarPath(member.avatar)
}

/**
 * @param {{ avatar?: string, avatarUrl?: string }} member
 * @param {string} baseApi
 */
export function resolveMemberAvatarUrl(member, baseApi = '') {
  if (!hasMemberPhoto(member)) return ''
  if (isNonEmptyAvatarPath(member.avatarUrl)) {
    const url = member.avatarUrl.trim()
    if (/^https?:\/\//i.test(url) || url.startsWith('/') || url.startsWith('data:')) {
      return url
    }
    return baseApi + url
  }
  const avatar = member.avatar.trim()
  if (/^https?:\/\//i.test(avatar) || avatar.startsWith('/') || avatar.startsWith('data:')) {
    return avatar
  }
  return baseApi + avatar
}

/** @param {{ nickName?: string, userName?: string, name?: string }} member */
export function resolveMemberDisplayName(member) {
  if (!member) return ''
  return member.nickName || member.userName || member.name || ''
}
