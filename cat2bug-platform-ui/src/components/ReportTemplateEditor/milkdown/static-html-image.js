/**
 * Parse / serialize materialized `<img>` HTML atoms (no Milkdown deps).
 */

/**
 * @param {string} value
 * @returns {string}
 */
export function escapeHtmlAttr(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

/**
 * Parse a single self-closing / void `<img ...>` HTML atom.
 * @param {string} html
 * @returns {{ src: string, alt: string, style: string } | null}
 */
export function parseImgHtml(html) {
  const trimmed = String(html || '').trim()
  const match = trimmed.match(/^<img\b([^>]*)\/?\s*>$/i)
  if (!match) {
    return null
  }
  const attrs = match[1] || ''
  const srcMatch = /(?:^|\s)src\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))/i.exec(attrs)
  const altMatch = /(?:^|\s)alt\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))/i.exec(attrs)
  const styleMatch = /(?:^|\s)style\s*=\s*(?:"([^"]*)"|'([^']*)')/i.exec(attrs)
  return {
    src: (srcMatch && (srcMatch[1] || srcMatch[2] || srcMatch[3])) || '',
    alt: (altMatch && (altMatch[1] || altMatch[2] || altMatch[3])) || '',
    style: (styleMatch && (styleMatch[1] || styleMatch[2])) || ''
  }
}

/**
 * @param {{ src: string, alt?: string, style?: string }} meta
 * @returns {string}
 */
export function serializeImgHtml(meta) {
  const src = meta?.src || ''
  const alt = meta?.alt || ''
  const style = meta?.style || ''
  let html = `<img src="${escapeHtmlAttr(src)}" alt="${escapeHtmlAttr(alt)}"`
  if (style) {
    html += ` style="${escapeHtmlAttr(style)}"`
  }
  html += ' />'
  return html
}
