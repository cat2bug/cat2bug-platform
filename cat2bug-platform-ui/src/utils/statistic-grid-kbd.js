/**
 * 统计模版选择区（flex-wrap 网格）方向键导航：按视觉位置找相邻模块。
 */

function rowThresholdFor(el) {
  if (!el) return 24
  return Math.max(el.getBoundingClientRect().height * 0.55, 24)
}

export function getFirstRowTop(els) {
  let min = Infinity
  els.forEach((el) => {
    const t = el.getBoundingClientRect().top
    if (t < min) min = t
  })
  return min
}

export function getLastRowTop(els) {
  let max = -Infinity
  els.forEach((el) => {
    const t = el.getBoundingClientRect().top
    if (t > max) max = t
  })
  return max
}

/** 当前项是否处于网格首行 / 末行 */
export function isOnFirstRow(els, index) {
  const cur = els[index]
  if (!cur) return false
  const curTop = cur.getBoundingClientRect().top
  const th = rowThresholdFor(cur)
  return !els.some((el, i) => i !== index && el.getBoundingClientRect().top < curTop - th)
}

export function isOnLastRow(els, index) {
  const cur = els[index]
  if (!cur) return false
  const curTop = cur.getBoundingClientRect().top
  const th = rowThresholdFor(cur)
  return !els.some((el, i) => i !== index && el.getBoundingClientRect().top > curTop + th)
}

export function getElementCenterX(el) {
  const r = el.getBoundingClientRect()
  return r.left + r.width / 2
}

/**
 * 在指定行（或首/末行）中找与 targetCx 水平位置最接近的项。
 * @param {'first'|'last'|null} rowMode
 */
export function findClosestIndexByX(els, targetCx, rowMode = null) {
  if (!els || !els.length) return -1
  let rowTop = null
  if (rowMode === 'first') rowTop = getFirstRowTop(els)
  if (rowMode === 'last') rowTop = getLastRowTop(els)
  const th = rowThresholdFor(els[0])
  let best = -1
  let bestDx = Infinity
  els.forEach((el, i) => {
    const r = el.getBoundingClientRect()
    if (rowTop != null && Math.abs(r.top - rowTop) > th) return
    const dx = Math.abs(r.left + r.width / 2 - targetCx)
    if (dx < bestDx) {
      bestDx = dx
      best = i
    }
  })
  return best
}

export function moveStatisticGridIndex(els, currentIndex, direction) {
  if (!els || !els.length || currentIndex < 0) return currentIndex
  const current = els[currentIndex]
  if (!current) return currentIndex
  const curRect = current.getBoundingClientRect()
  const curCx = curRect.left + curRect.width / 2
  const curCy = curRect.top + curRect.height / 2
  const rowThreshold = rowThresholdFor(current)

  let best = -1
  let bestDist = Infinity

  els.forEach((el, i) => {
    if (i === currentIndex) return
    const r = el.getBoundingClientRect()
    const cx = r.left + r.width / 2
    const cy = r.top + r.height / 2
    if (direction === 'left' && cx >= curCx - 6) return
    if (direction === 'right' && cx <= curCx + 6) return
    if (direction === 'up' && cy >= curCy - 6) return
    if (direction === 'down' && cy <= curCy + 6) return
    if (direction === 'left' || direction === 'right') {
      if (Math.abs(cy - curCy) > rowThreshold) return
    }
    const dx = cx - curCx
    const dy = cy - curCy
    const dist = (direction === 'left' || direction === 'right')
      ? Math.abs(dx) + Math.abs(dy) * 0.15
      : Math.abs(dy) + Math.abs(dx) * 0.15
    if (dist < bestDist) {
      bestDist = dist
      best = i
    }
  })
  return best >= 0 ? best : currentIndex
}
