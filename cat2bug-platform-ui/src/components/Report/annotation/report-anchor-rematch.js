/**
 * 报告批注锚点重匹配：正文可能被有权限成员改动，重新打开报告或正文变化时，
 * 尝试用 quote（+ 上下文）在当前渲染正文中重新定位批注。
 *
 * 策略：
 * - 块级单元：标题 / 段落 / 列表项 / 表格单元格 / 引用块 / 代码块（表格用 td/th，不用整行 tr）。
 * - selection：在命中块内再按 quote 包一层 <mark>，高亮选中文字而非整块。
 * - line：高亮整个块级元素。
 * - quote 多处出现时用 context_before / context_after 消歧；仍无法区分则取第一个候选。
 * - 找不到则 orphaned。
 */

export const ANNOTATABLE_SELECTOR = 'h1,h2,h3,h4,h5,h6,p,li,td,th,blockquote,pre'

const HIGHLIGHT_MARK_CLASS = 'report-annotation-highlight-mark'
const HIGHLIGHT_BLOCK_CLASS = 'report-annotation-highlight'

/**
 * 获取容器内可批注的"块级元素"，嵌套时只保留最内层（例如 li > p 只保留 p）。
 */
export function getAnnotatableBlocks(container) {
  if (!container) {
    return []
  }
  const all = Array.from(container.querySelectorAll(ANNOTATABLE_SELECTOR))
  return all.filter(el => !all.some(other => other !== el && el.contains(other)))
}

/**
 * 提取块级元素用于匹配/展示的文本。
 */
export function getBlockText(el) {
  if (!el) {
    return ''
  }
  if (el.tagName === 'TR') {
    const cells = Array.from(el.querySelectorAll('td,th'))
    return cells.map(c => (c.textContent || '').trim()).filter(Boolean).join(' | ')
  }
  return (el.textContent || '').trim()
}

/**
 * 尝试重新匹配一条批注的锚点。
 *
 * @param {Element} container 正文容器（#report-data）
 * @param {{quote:string, contextBefore?:string, contextAfter?:string, anchorKind?:string}} annotation
 * @returns {{matched:boolean, ambiguous:boolean, element:Element|null, anchorKind:string}}
 */
export function rematchAnnotation(container, annotation) {
  const quote = ((annotation && annotation.quote) || '').trim()
  const anchorKind = (annotation && annotation.anchorKind) || 'line'
  if (!container || !quote) {
    return { matched: false, ambiguous: false, element: null, anchorKind }
  }

  const blocks = getAnnotatableBlocks(container)
  const candidates = blocks.filter(b => getBlockText(b).includes(quote))

  if (candidates.length === 0) {
    return { matched: false, ambiguous: false, element: null, anchorKind }
  }
  if (candidates.length === 1) {
    return { matched: true, ambiguous: false, element: candidates[0], anchorKind }
  }

  const before = (annotation && annotation.contextBefore) || ''
  const after = (annotation && annotation.contextAfter) || ''
  if (before || after) {
    const disambiguated = candidates.find(b => {
      const text = getBlockText(b)
      const idx = text.indexOf(quote)
      if (idx < 0) {
        return false
      }
      const actualBefore = text.slice(Math.max(0, idx - before.length), idx)
      const actualAfter = text.slice(idx + quote.length, idx + quote.length + after.length)
      const beforeOk = !before || actualBefore.endsWith(before) || before.endsWith(actualBefore)
      const afterOk = !after || actualAfter.startsWith(after) || after.startsWith(actualAfter)
      return beforeOk && afterOk
    })
    if (disambiguated) {
      return { matched: true, ambiguous: true, element: disambiguated, anchorKind }
    }
  }

  return { matched: true, ambiguous: true, element: candidates[0], anchorKind }
}

/**
 * 根据纯文本偏移构造 Range。
 * @param {{ segments: { node: Text, start: number, end: number }[] }} index
 * @param {number} start
 * @param {number} end
 * @returns {Range|null}
 */
export function rangeFromPlainOffsets(index, start, end) {
  if (!index || !index.segments || typeof document === 'undefined' || typeof document.createRange !== 'function') {
    return null
  }
  const s = Math.max(0, Number(start) || 0)
  const e = Math.max(s, Number(end) || 0)
  let startNode = null
  let startOffset = 0
  let endNode = null
  let endOffset = 0
  index.segments.forEach(seg => {
    if (!startNode && s >= seg.start && s <= seg.end) {
      startNode = seg.node
      startOffset = s - seg.start
    }
    if (!endNode && e >= seg.start && e <= seg.end) {
      endNode = seg.node
      endOffset = e - seg.start
    }
  })
  if (!startNode || !endNode) {
    return null
  }
  const range = document.createRange()
  try {
    range.setStart(startNode, startOffset)
    range.setEnd(endNode, endOffset)
  } catch (err) {
    return null
  }
  return range
}

/**
 * 清除正文内批注高亮（块级 class + selection 的 mark 包裹）。
 * @param {Element} container
 */
export function clearAnnotationHighlights(container) {
  if (!container) {
    return
  }
  container.querySelectorAll('mark.' + HIGHLIGHT_MARK_CLASS).forEach(mark => {
    const parent = mark.parentNode
    if (!parent) {
      return
    }
    while (mark.firstChild) {
      parent.insertBefore(mark.firstChild, mark)
    }
    parent.removeChild(mark)
    if (typeof parent.normalize === 'function') {
      parent.normalize()
    }
  })
  container.querySelectorAll('.' + HIGHLIGHT_BLOCK_CLASS).forEach(el => {
    el.classList.remove(HIGHLIGHT_BLOCK_CLASS, 'is-active', 'is-flash')
  })
}

/**
 * 在块内把 quote 对应文字包进 <mark>，用于 selection 精确高亮。
 * @param {Element} blockEl
 * @param {{ quote?: string, contextBefore?: string, contextAfter?: string }} annotation
 * @param {{ active?: boolean }} [options]
 * @returns {HTMLElement|null} 创建的 mark；失败返回 null
 */
export function highlightQuoteInElement(blockEl, annotation, options = {}) {
  const quote = ((annotation && annotation.quote) || '').trim()
  if (!blockEl || !quote || typeof document === 'undefined') {
    return null
  }
  const index = buildPlainTextIndex(blockEl)
  const hit = rematchAnchor(annotation, index)
  if (hit.status !== 'matched' || hit.start == null || hit.end == null) {
    return null
  }
  const range = rangeFromPlainOffsets(index, hit.start, hit.end)
  if (!range || range.collapsed) {
    return null
  }
  const mark = document.createElement('mark')
  mark.className = HIGHLIGHT_MARK_CLASS + (options.active ? ' is-active' : '')
  try {
    range.surroundContents(mark)
  } catch (err) {
    try {
      const frag = range.extractContents()
      mark.appendChild(frag)
      range.insertNode(mark)
    } catch (err2) {
      return null
    }
  }
  return mark
}

/**
 * 计算容器内某文本节点位置在容器纯文本中的偏移量，用于提取选区上下文。
 */
export function textOffsetOf(container, node, offset) {
  if (!container || !node) {
    return 0
  }
  const range = document.createRange()
  range.selectNodeContents(container)
  try {
    range.setEnd(node, offset)
  } catch (e) {
    return 0
  }
  return range.toString().length
}

/**
 * 构建容器纯文本索引（供字符级辅助与单测）。
 * @param {Element|null} rootEl
 * @returns {{ text: string, segments: { node: Text, start: number, end: number }[] }}
 */
export function buildPlainTextIndex(rootEl) {
  if (!rootEl) {
    return { text: '', segments: [] }
  }
  const segments = []
  let text = ''
  const walker = document.createTreeWalker(rootEl, NodeFilter.SHOW_TEXT)
  let node = walker.nextNode()
  while (node) {
    const value = node.nodeValue || ''
    const start = text.length
    text += value
    segments.push({ node, start, end: text.length })
    node = walker.nextNode()
  }
  return { text, segments }
}

/**
 * @param {string} fullText
 * @param {number} start
 * @param {number} end
 * @param {number} [radius=20]
 */
export function captureContextAroundSelection(fullText, start, end, radius = 20) {
  const text = fullText || ''
  const s = Math.max(0, Number(start) || 0)
  const e = Math.max(s, Number(end) || 0)
  const r = Math.max(0, Number(radius) || 0)
  return {
    contextBefore: text.slice(Math.max(0, s - r), s),
    contextAfter: text.slice(e, e + r)
  }
}

/**
 * 字符级 quote 重匹配（单测 / 纯文本场景）；DOM 块级请用 rematchAnnotation。
 *
 * @param {{ quote?: string, contextBefore?: string, contextAfter?: string }|null} anchor
 * @param {string|{ text: string }} plainText
 * @returns {{ status: 'matched'|'orphaned', start: number|null, end: number|null }}
 */
export function rematchAnchor(anchor, plainText) {
  const text = typeof plainText === 'string' ? plainText : (plainText && plainText.text) || ''
  const quote = ((anchor && anchor.quote) || '').trim()
  if (!quote || !text) {
    return { status: 'orphaned', start: null, end: null }
  }
  const hits = []
  let from = 0
  while (from <= text.length) {
    const idx = text.indexOf(quote, from)
    if (idx < 0) {
      break
    }
    hits.push(idx)
    from = idx + Math.max(1, quote.length)
  }
  if (hits.length === 0) {
    return { status: 'orphaned', start: null, end: null }
  }
  if (hits.length === 1) {
    return { status: 'matched', start: hits[0], end: hits[0] + quote.length }
  }
  const before = (anchor && anchor.contextBefore) || ''
  const after = (anchor && anchor.contextAfter) || ''
  let best = hits[0]
  let bestScore = -1
  hits.forEach((idx) => {
    let score = 0
    if (before) {
      const actualBefore = text.slice(Math.max(0, idx - before.length), idx)
      if (actualBefore.endsWith(before) || before.endsWith(actualBefore)) {
        score += 2
      }
    }
    if (after) {
      const actualAfter = text.slice(idx + quote.length, idx + quote.length + after.length)
      if (actualAfter.startsWith(after) || after.startsWith(actualAfter)) {
        score += 2
      }
    }
    if (score > bestScore) {
      bestScore = score
      best = idx
    }
  })
  return { status: 'matched', start: best, end: best + quote.length }
}

/**
 * 提取选区的上下文快照（前后各 contextLength 个字符）。
 */
export function extractSelectionContext(container, range, contextLength = 60) {
  const fullText = container ? (container.textContent || '') : ''
  const startOffset = textOffsetOf(container, range.startContainer, range.startOffset)
  const endOffset = textOffsetOf(container, range.endContainer, range.endOffset)
  return {
    before: fullText.slice(Math.max(0, startOffset - contextLength), startOffset),
    after: fullText.slice(endOffset, endOffset + contextLength)
  }
}
