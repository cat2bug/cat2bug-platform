/**
 * 浏览器当前纵向可视范围（优先 visualViewport，适配移动端工具栏/键盘）。
 */
export function getViewportVerticalBounds() {
  const vv = typeof window !== 'undefined' ? window.visualViewport : null
  if (vv && typeof vv.height === 'number') {
    const top = Number.isFinite(vv.offsetTop) ? vv.offsetTop : 0
    return { top: Math.max(0, top), bottom: top + vv.height }
  }
  if (typeof window !== 'undefined') {
    return { top: 0, bottom: window.innerHeight }
  }
  return { top: 0, bottom: 0 }
}

/**
 * 竖线在 multipane-resizer 内与 flex align-items:flex-start 一致（贴顶绘制，避免与左右列表间顶部留白），
 * 再与可视区纵向求交取中点，使方块手柄落在「线条与视窗重叠段」的垂直中线。
 *
 * @param {HTMLElement} resizerEl .multipane-resizer 根元素
 * @param {number} lineHeightPx 与 CSS --marginTop 一致的竖线高度（px）
 * @returns {number|null} 手柄中心相对 resizer 顶部的 px（配合 top + translateY(-50%)）
 */
export function computeResizerHandleTopPx(resizerEl, lineHeightPx) {
  if (!resizerEl || typeof resizerEl.getBoundingClientRect !== 'function') return null
  const h = Number(lineHeightPx)
  if (!h || h < 1) return null

  const r = resizerEl.getBoundingClientRect()
  if (r.height < 1) return null

  const lineInsetTop = 0
  const lineTop = r.top + lineInsetTop
  const lineBottom = lineTop + h

  const { top: vTop, bottom: vBottom } = getViewportVerticalBounds()
  const visTop = Math.max(lineTop, vTop)
  const visBottom = Math.min(lineBottom, vBottom)

  let centerY
  if (visBottom > visTop) {
    centerY = (visTop + visBottom) / 2
  } else {
    centerY = lineTop + h / 2
  }

  return centerY - r.top
}
