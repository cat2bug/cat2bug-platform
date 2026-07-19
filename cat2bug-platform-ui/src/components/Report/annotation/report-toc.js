/**
 * 报告详情目录（TOC）工具：从渲染后的正文中提取标题、分配稳定锚点 id，并提供
 * 基于 IntersectionObserver 的 scroll-spy（当前章节高亮）。
 */

const HEADING_SELECTOR = 'h1,h2,h3,h4,h5,h6'
const TOC_ID_ATTR = 'data-report-toc-id'

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
      while (used.has(`report-heading-${candidate}`)) {
        candidate = `${base}-${++n}`
      }
      id = `report-heading-${candidate}`
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

/** 兼容单测命名；返回不含 el 的纯数据 */
export function extractAndAssignHeadingToc(rootEl) {
  return extractHeadings(rootEl).map(({ id, text, level }) => ({ id, text, level }))
}

/**
 * @param {{ root?: Element|null, headings: Array, onActiveChange: (id: string) => void }} opts
 */
export function createScrollSpy({ root, headings, onActiveChange }) {
  if (!headings || headings.length === 0 || typeof IntersectionObserver === 'undefined') {
    return { disconnect() {} }
  }

  const intersecting = new Map(headings.map(h => [h.el, false]))
  let activeId = headings[0].id

  function recompute() {
    let candidate = null
    for (const h of headings) {
      if (intersecting.get(h.el)) {
        candidate = h.id
      }
    }
    if (!candidate) {
      candidate = activeId
    }
    if (candidate !== activeId) {
      activeId = candidate
      onActiveChange(activeId)
    }
  }

  const observer = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      intersecting.set(entry.target, entry.isIntersecting)
    })
    recompute()
  }, {
    root: root || null,
    rootMargin: '0px 0px -70% 0px',
    threshold: 0
  })

  headings.forEach(h => observer.observe(h.el))
  onActiveChange(activeId)

  return {
    disconnect() {
      observer.disconnect()
    }
  }
}
