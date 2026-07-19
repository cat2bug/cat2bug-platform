/**
 * 计算滚动容器应设置的 scrollTop，使 targetEl 的顶部与 peerClientY（视口坐标）对齐。
 * 用于：点击右侧评论后，把正文锚点滚到与该评论相同的屏幕高度。
 *
 * @param {Element} scrollRoot 可滚动容器（如 .report-detail-main）
 * @param {Element} targetEl 正文中的高亮锚点元素
 * @param {number} peerClientY 对标元素（评论卡片）的 getBoundingClientRect().top
 * @returns {number|null} 新的 scrollTop；参数无效时返回 null
 */
export function computeAlignScrollTop(scrollRoot, targetEl, peerClientY) {
  if (!scrollRoot || !targetEl || typeof peerClientY !== 'number' || Number.isNaN(peerClientY)) {
    return null
  }
  const targetRect = targetEl.getBoundingClientRect()
  const delta = targetRect.top - peerClientY
  const next = scrollRoot.scrollTop + delta
  const max = Math.max(0, scrollRoot.scrollHeight - scrollRoot.clientHeight)
  return Math.min(Math.max(0, next), max)
}

/**
 * 将正文锚点平滑滚动到与对标元素同高。
 *
 * @param {Element} scrollRoot
 * @param {Element} targetEl
 * @param {Element|null} peerEl 右侧评论 DOM；缺省时退回 block: 'center'
 */
export function scrollTargetToPeerHeight(scrollRoot, targetEl, peerEl) {
  if (!targetEl) {
    return
  }
  if (!scrollRoot || !peerEl || typeof peerEl.getBoundingClientRect !== 'function') {
    if (typeof targetEl.scrollIntoView === 'function') {
      targetEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
    return
  }
  const peerTop = peerEl.getBoundingClientRect().top
  const nextTop = computeAlignScrollTop(scrollRoot, targetEl, peerTop)
  if (nextTop == null) {
    return
  }
  if (typeof scrollRoot.scrollTo === 'function') {
    scrollRoot.scrollTo({ top: nextTop, behavior: 'smooth' })
  } else {
    scrollRoot.scrollTop = nextTop
  }
}
