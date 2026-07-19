/**
 * 报告正文字数：去掉常见 Markdown 标记后，统计非空白字符（中文「字数」习惯）。
 * @param {string|null|undefined} source
 * @returns {number}
 */
export function countReportChars(source) {
  let text = String(source == null ? '' : source)
  text = text.replace(/^#{1,6}\s+/gm, '')
  text = text.replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
  text = text.replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
  text = text.replace(/(\*\*|__|~~|`)/g, '')
  text = text.replace(/^\s*[-*+]\s+/gm, '')
  text = text.replace(/^\s*\d+\.\s+/gm, '')
  text = text.replace(/\|/g, '')
  text = text.replace(/^\s*:?-+:?\s*(?:\|\s*:?-+:?\s*)+$/gm, '')
  return text.replace(/\s+/g, '').length
}
