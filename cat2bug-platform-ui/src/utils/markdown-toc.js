/**
 * 从渲染后的 Markdown 正文中提取标题并分配稳定锚点 id（供 Git 浏览等 TOC 使用）。
 */

const HEADING_SELECTOR = 'h1,h2,h3,h4,h5,h6'
const TOC_ID_ATTR = 'data-md-toc-id'

function slugify(text) {
  return (text || '')
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9\u4e00-\u9fa5]+/g, '-')
    .replace(/^-+|-+$/g, '')
}

/**
 * 扫描容器内的 h1-h6，写入稳定 id 并返回 TOC 条目列表。
 * @param {Element|null|undefined} container
 * @returns {{ id: string, text: string, level: number, el?: Element }[]}
 */
export function extractHeadings(container) {
  if (!container) {
    return []
  }
  const nodes = Array.from(container.querySelectorAll(HEADING_SELECTOR))
  const used = new Set()
  return nodes.map((el, index) => {
    let id = el.getAttribute(TOC_ID_ATTR) || el.id || ''
    if (!id || used.has(id)) {
      const base = slugify(el.textContent) || `section-${index + 1}`
      let candidate = base
      let n = 1
      while (used.has(`md-heading-${candidate}`)) {
        candidate = `${base}-${++n}`
      }
      id = `md-heading-${candidate}`
    }
    used.add(id)
    el.setAttribute(TOC_ID_ATTR, id)
    if (!el.id) {
      el.id = id
    }
    return {
      id,
      text: (el.textContent || '').trim(),
      level: Number((el.tagName || 'H1').charAt(1)) || 1,
      el
    }
  })
}
