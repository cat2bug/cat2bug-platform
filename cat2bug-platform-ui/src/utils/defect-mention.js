/**
 * PR / 文本中的缺陷提及：# + 项目内编号。
 * 锚点形如 #cat2bug-defect-61，经 DOMPurify 后仍可点击委托打开抽屉。
 */

export const DEFECT_MENTION_HASH_PREFIX = 'cat2bug-defect-'

const FENCE_SPLIT = /(```[\s\S]*?```|~~~[\s\S]*?~~~)/g
const MENTION_RE = /(^|[^A-Za-z0-9_/])#(\d+)\b/g

/**
 * @param {string} text
 * @returns {string}
 */
export function linkDefectMentionsInMarkdown(text) {
  if (text == null || text === '') return ''
  const source = String(text)
  const parts = source.split(FENCE_SPLIT)
  return parts.map((part, index) => {
    if (index % 2 === 1) return part
    return part.replace(MENTION_RE, (full, prefix, num) => `${prefix}[#${num}](#${DEFECT_MENTION_HASH_PREFIX}${num})`)
  }).join('')
}

/**
 * 将标题拆成文本 / 提及片段，供模板渲染可点击链接。
 * @param {string} title
 * @returns {Array<{ type: 'text'|'mention', value: string, num?: number }>}
 */
export function splitDefectMentions(title) {
  if (title == null || title === '') return []
  const source = String(title)
  const segments = []
  let last = 0
  const re = /(^|[^A-Za-z0-9_/])#(\d+)\b/g
  let m
  while ((m = re.exec(source)) != null) {
    const prefix = m[1] || ''
    const num = m[2]
    const mentionStart = m.index + prefix.length
    if (last < mentionStart) {
      segments.push({ type: 'text', value: source.slice(last, mentionStart) })
    }
    segments.push({ type: 'mention', value: `#${num}`, num: Number(num) })
    last = mentionStart + num.length + 1
  }
  if (last < source.length) {
    segments.push({ type: 'text', value: source.slice(last) })
  }
  return segments.length ? segments : [{ type: 'text', value: source }]
}

/**
 * @param {string | null | undefined} href
 * @returns {number | null}
 */
export function parseDefectMentionHref(href) {
  if (href == null) return null
  const raw = String(href).trim()
  const hash = raw.startsWith('#') ? raw.slice(1) : raw
  if (!hash.startsWith(DEFECT_MENTION_HASH_PREFIX)) return null
  const num = Number(hash.slice(DEFECT_MENTION_HASH_PREFIX.length))
  return Number.isFinite(num) && num > 0 ? num : null
}
