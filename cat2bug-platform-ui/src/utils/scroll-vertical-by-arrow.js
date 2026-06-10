/**
 * ⌘/Ctrl + ↑/↓：按视口比例滚动垂直可滚动容器。
 * @returns {boolean} 是否已处理
 */
export function scrollVerticalContainerByArrow(container, key, options) {
  if (!container || !key) return false
  if (key !== 'ArrowUp' && key !== 'ArrowDown') return false
  if (container.scrollHeight <= container.clientHeight + 2) return false
  const ratio = (options && options.stepRatio) != null ? options.stepRatio : 0.4
  const minStep = (options && options.minStep) != null ? options.minStep : 120
  const delta = Math.max(minStep, Math.round(container.clientHeight * ratio))
  const top = key === 'ArrowDown' ? delta : -delta
  if (options && options.behavior === 'instant') {
    container.scrollTop = Math.max(0, Math.min(container.scrollTop + top, container.scrollHeight - container.clientHeight))
  } else {
    container.scrollBy({ top, behavior: 'smooth' })
  }
  return true
}
