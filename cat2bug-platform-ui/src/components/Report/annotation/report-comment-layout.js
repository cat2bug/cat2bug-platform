/**
 * 报告侧栏评论：按正文锚点 Y 坐标排布（非列表堆叠）。
 */

/**
 * 锚点相对滚动内容顶部的偏移。
 * 元素若不在滚动容器内（DOM 已重建的陈旧引用），返回 null。
 * @param {Element} scrollRoot
 * @param {Element} element
 * @returns {number|null}
 */
export function getContentOffsetTop(scrollRoot, element) {
  if (!scrollRoot || !element || typeof element.getBoundingClientRect !== 'function') {
    return null
  }
  if (typeof scrollRoot.contains === 'function' && !scrollRoot.contains(element)) {
    return null
  }
  const rootRect = scrollRoot.getBoundingClientRect()
  const elRect = element.getBoundingClientRect()
  return elRect.top - rootRect.top + (scrollRoot.scrollTop || 0)
}

/**
 * 限制评论卡片高度，避免量到异常大值后把后续评论全部挤到下方。
 * @param {number} height
 * @param {{ min?: number, max?: number }} [opts]
 * @returns {number}
 */
export function clampCommentCardHeight(height, opts = {}) {
  const min = opts.min != null ? opts.min : 48
  const max = opts.max != null ? opts.max : 280
  const h = Number(height)
  if (!Number.isFinite(h) || h <= 0) {
    return min
  }
  return Math.min(max, Math.max(min, h))
}

/**
 * 将期望 top 按卡片高度做碰撞推开。
 * 仅当「按锚点放置会与上一条重叠」时才下推，尽量保持与正文 Y 对齐。
 * @param {Array<{ id: string|number, top: number, height: number }>} items
 * @param {number} [gap=8]
 * @returns {Array<{ id: string|number, top: number }>}
 */
export function resolveCommentCollisions(items, gap = 8) {
  const sorted = (items || [])
    .slice()
    .sort((a, b) => a.top - b.top || String(a.id).localeCompare(String(b.id)))
  let prevBottom = -Infinity
  return sorted.map(item => {
    const height = clampCommentCardHeight(item.height)
    let top = item.top
    if (prevBottom !== -Infinity && top < prevBottom + gap) {
      top = prevBottom + gap
    }
    prevBottom = top + height
    return { id: item.id, top }
  })
}

/**
 * 根据 rematch 结果生成评论 top 映射（尚未做碰撞推开前的期望值）。
 * @param {Array<{ root: { commentId: any }, matched?: boolean, element?: Element|null }>} threads
 * @param {Element} scrollRoot
 * @param {number} [viewportDelta=0] scrollRoot.top - commentsBody.top，用于对齐视口
 * @returns {Record<string, number>}
 */
export function buildDesiredCommentTops(threads, scrollRoot, viewportDelta = 0) {
  const tops = {}
  ;(threads || []).forEach(thread => {
    const id = thread && thread.root ? thread.root.commentId : null
    if (id == null) {
      return
    }
    if (thread.matched && thread.element) {
      const offset = getContentOffsetTop(scrollRoot, thread.element)
      if (offset != null) {
        tops[String(id)] = Math.max(0, offset + viewportDelta)
      }
    }
  })
  return tops
}

/**
 * 草稿优先落在正文锚点 Y；若与已有评论卡片重叠则下推，不覆盖已有块。
 * @param {number} desiredTop
 * @param {Array<{ top: number, height: number }>} occupied
 * @param {number} [draftHeight=120]
 * @param {number} [gap=8]
 * @returns {number}
 */
export function resolveDraftTopAmongComments(desiredTop, occupied, draftHeight = 120, gap = 8) {
  let top = Math.max(0, Number(desiredTop) || 0)
  const h = clampCommentCardHeight(draftHeight, { min: 72, max: 360 })
  const sorted = (occupied || [])
    .slice()
    .sort((a, b) => a.top - b.top)
  for (const item of sorted) {
    const ih = clampCommentCardHeight(item.height)
    const itemBottom = item.top + ih
    const draftBottom = top + h
    const overlaps = top < itemBottom + gap && draftBottom + gap > item.top
    if (overlaps) {
      top = itemBottom + gap
    }
  }
  return top
}

/**
 * 用 quote 在可见正文中找锚点元素（编辑态选区消失后的兜底）。
 * @param {Element|null|undefined} root
 * @param {string} quote
 * @returns {Element|null}
 */
export function findElementByQuote(root, quote) {
  if (!root || !quote) {
    return null
  }
  const q = String(quote).trim()
  if (!q) {
    return null
  }
  const nodes = root.querySelectorAll
    ? root.querySelectorAll('h1,h2,h3,h4,h5,h6,p,li,td,th,blockquote,pre')
    : []
  for (const el of Array.from(nodes)) {
    const text = (el.textContent || '').trim()
    if (text && text.indexOf(q) !== -1) {
      return el
    }
  }
  return null
}
