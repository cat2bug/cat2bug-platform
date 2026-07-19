/**
 * 报告详情目录（TOC）工具：从渲染后的正文中提取标题、分配稳定锚点 id，并提供
 * 基于 IntersectionObserver 的 scroll-spy（当前章节高亮）。
 * 另提供 Markdown 标题块拆分 / 拖拽重排，供目录拖动同步正文。
 */

const HEADING_SELECTOR = 'h1,h2,h3,h4,h5,h6'
const TOC_ID_ATTR = 'data-report-toc-id'
const ATX_HEADING_RE = /^(#{1,6})\s+(.+?)(?:\s+#*)?\s*$/
const FENCE_RE = /^(`{3,}|~{3,})/

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
 * 将 Markdown 拆成「文首前言 + 标题块」。
 * 每个标题块 = 该标题行 + 直到下一标题之前的正文（代码围栏内的 # 不算标题）。
 * @param {string} source
 * @returns {{ preamble: string[], headingBlocks: Array<{ level: number, title: string, lines: string[] }> }}
 */
export function splitMarkdownHeadingBlocks(source) {
  const lines = String(source == null ? '' : source).split('\n')
  const preamble = []
  const headingBlocks = []
  let current = null
  let inFence = false

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    if (FENCE_RE.test(line.trim())) {
      inFence = !inFence
      if (current) {
        current.lines.push(line)
      } else {
        preamble.push(line)
      }
      continue
    }
    const m = !inFence ? ATX_HEADING_RE.exec(line) : null
    if (m) {
      if (current) {
        headingBlocks.push(current)
      }
      current = {
        level: m[1].length,
        title: m[2].trim(),
        lines: [line]
      }
      continue
    }
    if (current) {
      current.lines.push(line)
    } else {
      preamble.push(line)
    }
  }
  if (current) {
    headingBlocks.push(current)
  }
  return { preamble, headingBlocks }
}

/**
 * 按目录拖拽结果重排 Markdown 标题块（含各自后续正文）。
 * @param {string} source
 * @param {number} fromIndex 原目录下标
 * @param {number} toIndex 目标目录下标
 * @returns {string}
 */
export function reorderMarkdownHeadingBlocks(source, fromIndex, toIndex) {
  const raw = String(source == null ? '' : source)
  if (!Number.isInteger(fromIndex) || !Number.isInteger(toIndex) || fromIndex === toIndex) {
    return raw
  }
  const { preamble, headingBlocks } = splitMarkdownHeadingBlocks(raw)
  if (
    fromIndex < 0 ||
    toIndex < 0 ||
    fromIndex >= headingBlocks.length ||
    toIndex >= headingBlocks.length
  ) {
    return raw
  }
  const next = headingBlocks.slice()
  const [moved] = next.splice(fromIndex, 1)
  next.splice(toIndex, 0, moved)
  const parts = []
  if (preamble.length) {
    parts.push(preamble.join('\n'))
  }
  next.forEach(block => {
    parts.push(block.lines.join('\n'))
  })
  return parts.join('\n')
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
