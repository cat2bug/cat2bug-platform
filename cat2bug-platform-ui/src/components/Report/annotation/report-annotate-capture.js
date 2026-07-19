/**
 * 从当前 Selection 或块级元素捕获批注锚点信息：quote + 上下文快照 + 可选的最近标题
 * id 作为 anchorHint（仅辅助，不参与重匹配判定）。
 */

import { buildPlainTextIndex, captureContextAroundSelection } from './report-anchor-rematch'

const DEFAULT_CONTEXT_RADIUS = 20
const LINE_BLOCK_SELECTOR = 'p, li, tr, td, th, h1, h2, h3, h4, h5, h6, blockquote, pre'
const HEADING_SELECTOR = 'h1, h2, h3, h4, h5, h6'

/**
 * 从任意起点元素向上查找最近的整行/整块容器（p、li、tr、td 等）。
 * @param {Element|null|undefined} el
 * @param {Element|null|undefined} rootEl
 * @returns {Element|null}
 */
export function resolveAnnotationBlockElement(el, rootEl) {
  let node = el
  while (node && node.nodeType === 1) {
    if (typeof node.matches === 'function' && node.matches(LINE_BLOCK_SELECTOR)) {
      return node
    }
    if (node === rootEl) {
      break
    }
    node = node.parentElement
  }
  return null
}

/**
 * 找到 rootEl 内位于 node 之前、离 node 最近的标题 id，用作 anchorHint。
 * @param {Element|null|undefined} node
 * @param {Element|null|undefined} rootEl
 * @returns {string|null}
 */
function findNearestHeadingId(node, rootEl) {
  if (!node || !rootEl || typeof rootEl.querySelectorAll !== 'function') {
    return null
  }
  const headings = rootEl.querySelectorAll(HEADING_SELECTOR)
  let nearestId = null
  for (let i = 0; i < headings.length; i++) {
    const heading = headings[i]
    if (heading === node) {
      break
    }
    const position = heading.compareDocumentPosition(node)
    // eslint-disable-next-line no-bitwise
    const headingPrecedesNode = (position & Node.DOCUMENT_POSITION_FOLLOWING) !== 0
    if (!headingPrecedesNode) {
      break
    }
    const id = heading.getAttribute('id')
    if (id) {
      nearestId = id
    }
  }
  return nearestId
}

/**
 * 去除 rawQuote 两端空白，并把区间同步收缩，保证收缩后的 quote 仍是
 * plainText.slice(adjustedStart, adjustedEnd) 的精确子串。
 * @param {string} plainText
 * @param {number} start
 * @param {number} end
 * @returns {{ quote: string, start: number, end: number }}
 */
function trimQuoteRange(plainText, start, end) {
  const raw = plainText.slice(start, end)
  const leadTrim = raw.match(/^\s*/)[0].length
  const trailTrim = raw.match(/\s*$/)[0].length
  const adjustedStart = start + leadTrim
  const adjustedEnd = end - trailTrim
  return {
    quote: plainText.slice(adjustedStart, Math.max(adjustedStart, adjustedEnd)),
    start: adjustedStart,
    end: Math.max(adjustedStart, adjustedEnd)
  }
}

/**
 * 由 [start, end) 区间与纯文本索引拼出 quote + 上下文快照；quote 为空返回 null。
 * @param {{ text: string }} index buildPlainTextIndex 结果
 * @param {number} start
 * @param {number} end
 * @param {number} radius
 * @returns {{ quote: string, contextBefore: string, contextAfter: string }|null}
 */
function deriveQuoteAndContext(index, start, end, radius) {
  const { quote, start: qStart, end: qEnd } = trimQuoteRange(index.text, start, end)
  if (!quote) {
    return null
  }
  const { contextBefore, contextAfter } = captureContextAroundSelection(index.text, qStart, qEnd, radius)
  return { quote, contextBefore, contextAfter }
}

/**
 * 在 index.segments 中找出被 el 包含的文本节点，返回其覆盖的 [start, end) 区间。
 * @param {Element} el
 * @param {{ segments: { node: Text, start: number, end: number }[] }} index
 * @returns {{ start: number, end: number }|null}
 */
function locateElementTextRange(el, index) {
  let start = null
  let end = null
  for (const segment of index.segments) {
    if (el.contains(segment.node)) {
      if (start === null) {
        start = segment.start
      }
      end = segment.end
    }
  }
  if (start === null) {
    return null
  }
  return { start, end }
}

/**
 * 把 Range 端点（node + offset）换算成 index.text 中的字符下标；仅在容器为
 * 文本节点时精确定位，其余情况返回 null（由调用方回退为空上下文）。
 * @param {Node} container
 * @param {number} offset
 * @param {{ segments: { node: Text, start: number, end: number }[] }} index
 * @returns {number|null}
 */
function resolveOffsetInIndex(container, offset, index) {
  if (!container) {
    return null
  }
  if (container.nodeType === 3) {
    for (const segment of index.segments) {
      if (segment.node === container) {
        return segment.start + offset
      }
    }
    return null
  }
  // 容器是元素节点：offset 表示子节点下标，取该子节点子树内的文本区间
  const referenceChild = offset > 0 ? container.childNodes[offset - 1] : null
  if (!referenceChild) {
    for (const segment of index.segments) {
      if (container.contains(segment.node)) {
        return segment.start
      }
    }
    return null
  }
  let end = null
  for (const segment of index.segments) {
    if (referenceChild === segment.node || (referenceChild.contains && referenceChild.contains(segment.node))) {
      end = segment.end
    }
  }
  return end
}

/**
 * 对整行 / 整块元素（p、li、tr、td、标题等）取 textContent 作为 quote，捕获批注锚点。
 * @param {Element|null|undefined} el 目标块级元素，或其任意子节点（会向上查找最近的整块容器）
 * @param {Element|null|undefined} rootEl 报告正文容器
 * @param {{ radius?: number }} [options]
 * @returns {{ anchorKind: 'line', quote: string, contextBefore: string, contextAfter: string, anchorHint: string|null }|null}
 */
export function captureLineAnnotation(el, rootEl, options = {}) {
  const blockEl = resolveAnnotationBlockElement(el, rootEl)
  if (!blockEl) {
    return null
  }
  const radius = options.radius != null ? options.radius : DEFAULT_CONTEXT_RADIUS
  const index = buildPlainTextIndex(rootEl || blockEl)
  const range = locateElementTextRange(blockEl, index)
  if (!range) {
    return null
  }
  const captured = deriveQuoteAndContext(index, range.start, range.end, radius)
  if (!captured) {
    return null
  }
  return {
    anchorKind: 'line',
    quote: captured.quote,
    contextBefore: captured.contextBefore,
    contextAfter: captured.contextAfter,
    anchorHint: findNearestHeadingId(blockEl, rootEl)
  }
}

/**
 * 对当前文字选区捕获批注锚点：quote 为选区文本（去除两端空白），上下文取自选区
 * 所在纯文本索引中的实际前后字符。
 * @param {Selection|null|undefined} selection window.getSelection() 返回值
 * @param {Element|null|undefined} rootEl 报告正文容器
 * @param {{ radius?: number }} [options]
 * @returns {{ anchorKind: 'selection', quote: string, contextBefore: string, contextAfter: string, anchorHint: string|null }|null}
 */
export function captureSelectionAnnotation(selection, rootEl, options = {}) {
  if (!selection || selection.isCollapsed || !selection.rangeCount) {
    return null
  }
  const range = selection.getRangeAt(0)
  if (!range || range.collapsed) {
    return null
  }
  const radius = options.radius != null ? options.radius : DEFAULT_CONTEXT_RADIUS
  const index = buildPlainTextIndex(rootEl || range.commonAncestorContainer)
  const start = resolveOffsetInIndex(range.startContainer, range.startOffset, index)
  const end = resolveOffsetInIndex(range.endContainer, range.endOffset, index)
  if (start === null || end === null || end <= start) {
    return null
  }
  const captured = deriveQuoteAndContext(index, start, end, radius)
  if (!captured) {
    return null
  }
  const anchorNode = range.startContainer.nodeType === 1 ? range.startContainer : range.startContainer.parentElement
  return {
    anchorKind: 'selection',
    quote: captured.quote,
    contextBefore: captured.contextBefore,
    contextAfter: captured.contextAfter,
    anchorHint: findNearestHeadingId(anchorNode, rootEl)
  }
}

/**
 * 统一入口：优先使用非折叠的文字选区捕获批注；若无有效选区，则回退到块级元素捕获整行。
 * @param {{ selection?: Selection|null, blockEl?: Element|null }} input
 * @param {Element|null|undefined} rootEl 报告正文容器
 * @param {{ radius?: number }} [options]
 * @returns {{ anchorKind: 'line'|'selection', quote: string, contextBefore: string, contextAfter: string, anchorHint: string|null }|null}
 */
export function captureAnnotationFromInput(input, rootEl, options = {}) {
  const selection = input && input.selection
  if (selection && !selection.isCollapsed && selection.rangeCount) {
    const bySelection = captureSelectionAnnotation(selection, rootEl, options)
    if (bySelection) {
      return bySelection
    }
  }
  const blockEl = input && input.blockEl
  if (blockEl) {
    return captureLineAnnotation(blockEl, rootEl, options)
  }
  return null
}
