import { escapeHtml, sanitizeHtml } from './sanitize-html'

const LINE_STYLE = 'flex:1;display: flex;flex-direction: row;align-items: center;flex-wrap: wrap;'

/** contenteditable 输入框提交的是 innerHTML，已有实体（如 &lt; &nbsp;）保持原样，只转义裸露的标记字符 */
const BARE_AMPERSAND = /&(?!(?:[a-zA-Z][a-zA-Z0-9]{1,31}|#\d{1,7}|#x[0-9a-fA-F]{1,6});)/g

function escapeCommentText(text) {
  return text
    .replace(BARE_AMPERSAND, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function normalizeEditableMarkup(text) {
  return text
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<div>/gi, '\n')
    .replace(/<\/div>/gi, '')
}

/**
 * 评论内容转为可 v-html 的 HTML：先转义，再按行包装、替换 `[img:名称]` 表情，最后清洗。
 * @param {string} content
 * @param {Map<string, string>} emojiMap 表情名 → 图片地址
 * @returns {string}
 */
export function commentToHtml(content, emojiMap) {
  const text = escapeCommentText(normalizeEditableMarkup(String(content || '')))
  let html = text.split('\n').map(line => `<div style="${LINE_STYLE}">${line}</div>`).join('')
  const flags = Array.from(new Set(text.match(/\[.*?\]/g) || []))
  flags.forEach(flag => {
    const inner = flag.substring(1, flag.length - 1)
    let params = inner.split(':')
    if (params.length === 1) {
      params = ['img', inner]
    }
    const name = params[1]
    if (emojiMap && emojiMap.has(name)) {
      html = html.replaceAll(flag, `<img alt="${escapeHtml(name)}" style="width:30px;" src="${escapeHtml(emojiMap.get(name))}" />`)
    }
  })
  return sanitizeHtml(html)
}
