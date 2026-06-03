/**
 * 交付物全路径展示：过长时前层折叠为 .../...，末层保留尾部字符（不用 * 号）。
 * @param {string} fullPath
 * @param {number} maxLength
 * @returns {{ short: string, full: string }}
 */
export function formatDeliverablePath(fullPath, maxLength = 32) {
  const full = (fullPath == null ? '' : String(fullPath)).trim()
  if (!full) {
    return { short: '', full: '' }
  }
  if (full.length <= maxLength) {
    return { short: full, full }
  }

  const parts = full.split('/').filter(Boolean)
  if (parts.length <= 1) {
    return { short: truncateWithLeadingEllipsis(full, maxLength), full }
  }

  const last = parts[parts.length - 1]
  const prefix = parts.length >= 3 ? '.../...' : '.../'
  const candidate = prefix + last
  if (candidate.length <= maxLength) {
    return { short: candidate, full }
  }

  const room = maxLength - prefix.length
  if (room <= 0) {
    return { short: prefix.slice(0, maxLength), full }
  }
  return { short: prefix + last.slice(-room), full }
}

/** 单层名称过长：前导 ... + 保留末尾 */
function truncateWithLeadingEllipsis(text, maxLength) {
  if (text.length <= maxLength) {
    return text
  }
  const prefix = '...'
  const room = maxLength - prefix.length
  if (room <= 0) {
    return prefix.slice(0, maxLength)
  }
  return prefix + text.slice(-room)
}

/** 是否像交付物多层路径 */
export function looksLikeDeliverablePath(value) {
  if (value == null || value === '') return false
  const s = String(value).trim()
  return s.includes('/')
}
