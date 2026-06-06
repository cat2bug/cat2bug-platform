import { normalizeKey } from '@/plugins/shortcut/keymap'

/** 与 page-action-hints 一致：不可用于行/工具栏动态分配 */
export const ROW_KBD_RESERVED = Object.freeze(
  new Set(['A', 'C', 'M', 'N', 'Q', 'T', 'V', 'X', 'Z'])
)

const LETTER_POOL = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')

/** 为可见行顺序分配快捷键：1–9，再用未占用的字母 */
export function assignRowHintLetters(count, usedLetters = new Set()) {
  const pool = []
  for (let d = 1; d <= 9; d++) {
    const s = String(d)
    if (!usedLetters.has(s)) pool.push(s)
  }
  LETTER_POOL.forEach((ch) => {
    if (ROW_KBD_RESERVED.has(ch)) return
    if (usedLetters.has(ch)) return
    pool.push(ch)
  })
  return pool.slice(0, count)
}

export function isRowIntersectingContainer(el, container, minPx = 4) {
  if (!el || !container) return false
  const er = el.getBoundingClientRect()
  const cr = container.getBoundingClientRect()
  if (er.width <= 0 && er.height <= 0) return false
  const overlapTop = Math.max(er.top, cr.top)
  const overlapBottom = Math.min(er.bottom, cr.bottom)
  return overlapBottom - overlapTop >= minPx
}

/** Excel：扣除 sticky 表头/表尾后的数据可视区 */
export function getExcelScrollViewportRect(tableContent) {
  if (!tableContent || typeof tableContent.getBoundingClientRect !== 'function') return null
  const cr = tableContent.getBoundingClientRect()
  let top = cr.top
  let bottom = cr.bottom
  const thead = tableContent.querySelector('thead')
  if (thead) {
    thead.querySelectorAll('tr').forEach((tr) => {
      const hr = tr.getBoundingClientRect()
      if (hr.height > 0 && hr.bottom > top && hr.top < bottom) {
        top = Math.max(top, hr.bottom)
      }
    })
  }
  const tfoot = tableContent.querySelector('tfoot')
  if (tfoot) {
    tfoot.querySelectorAll('tr').forEach((tr) => {
      const fr = tr.getBoundingClientRect()
      if (fr.height > 0 && fr.top < bottom && fr.bottom > top) {
        bottom = Math.min(bottom, fr.top)
      }
    })
  }
  if (bottom - top < 4) return null
  return { top, bottom, left: cr.left, right: cr.right }
}

export function isElementInViewportRect(el, viewportRect, minPx = 4) {
  if (!el || !viewportRect) return false
  const er = el.getBoundingClientRect()
  if (er.width <= 0 && er.height <= 0) return false
  const overlapTop = Math.max(er.top, viewportRect.top)
  const overlapBottom = Math.min(er.bottom, viewportRect.bottom)
  const overlapLeft = Math.max(er.left, viewportRect.left)
  const overlapRight = Math.min(er.right, viewportRect.right)
  return overlapBottom - overlapTop >= minPx && overlapRight - overlapLeft >= minPx
}

export function isExcelRowVisibleInViewport(tr, tableContent, minPx = 4) {
  const viewport = getExcelScrollViewportRect(tableContent)
  if (!viewport) return false
  const probe = tr.querySelector('td.first-col') || tr
  return isElementInViewportRect(probe, viewport, minPx)
}

function mergeClientRects(rects) {
  let top = Infinity
  let left = Infinity
  let bottom = -Infinity
  let right = -Infinity
  ;(rects || []).forEach((r) => {
    if (!r || r.width <= 0 || r.height <= 0) return
    top = Math.min(top, r.top)
    left = Math.min(left, r.left)
    bottom = Math.max(bottom, r.bottom)
    right = Math.max(right, r.right)
  })
  if (!isFinite(top)) return null
  return { top, left, bottom, right, width: right - left, height: bottom - top }
}

/** 元素内可见文字包围盒（用于序号 span，避免整格 td 当锚点） */
export function getElementTextRect(el) {
  if (!el || typeof document === 'undefined') return null
  try {
    const range = document.createRange()
    range.selectNodeContents(el)
    let rects = Array.from(range.getClientRects()).filter((r) => r.width > 0 && r.height > 0)
    if (!rects.length && typeof document.createTreeWalker === 'function') {
      const collected = []
      const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT, null)
      let node = walker.nextNode()
      while (node) {
        const text = node.textContent || ''
        if (text.trim()) {
          const tr = document.createRange()
          tr.selectNodeContents(node)
          collected.push(...Array.from(tr.getClientRects()))
        }
        node = walker.nextNode()
      }
      rects = collected.filter((r) => r.width > 0 && r.height > 0)
    }
    return mergeClientRects(rects)
  } catch (e) {
    return null
  }
}

/** Excel 序号文字 span（徽标对齐文字右下角，非整格 td） */
export function resolveExcelRowHintAnchor(tr) {
  if (!tr) return null
  return tr.querySelector('td.first-col > span') || tr.querySelector('td.first-col')
}

/** 徽标定位用矩形：优先文字包围盒 */
export function resolveExcelRowHintAnchorRect(anchor) {
  if (!anchor) return null
  return getElementTextRect(anchor) || anchor.getBoundingClientRect()
}

export function collectHintLettersFromToolbar(hints) {
  const used = new Set()
  ;(hints || []).forEach((item) => {
    if (!item) return
    if (item.visible === false) return
    if (typeof item.visible === 'function' && !item.visible()) return
    const letter = normalizeKey(item.letter)
    if (letter) used.add(letter)
  })
  return used
}

/** 从 Excel 单元格 id（id-{row$id}-{field}）解析 $id */
export function parseExcelRowKeyFromTdId(tdId) {
  if (!tdId || !String(tdId).startsWith('id-')) return null
  const rest = String(tdId).slice(3)
  const dash = rest.lastIndexOf('-')
  if (dash <= 0) return null
  return rest.slice(0, dash)
}

export function findExcelSheetRow(sheetRows, rowKey) {
  if (!rowKey || !Array.isArray(sheetRows)) return null
  return sheetRows.find((r) => String(r.$id) === String(rowKey)) || null
}

/** 主表体滚动容器（排除 fixed 列镜像表体，避免重复行） */
export function getDefectTableScrollBody(tableRoot) {
  if (!tableRoot || !tableRoot.querySelectorAll) return null
  const wraps = Array.from(tableRoot.querySelectorAll('.el-table__body-wrapper'))
  if (!wraps.length) return null
  return wraps.find((w) => !w.closest('.el-table__fixed, .el-table__fixed-right')) || wraps[0]
}

/** 表格行快捷键徽标锚点：编号列 > 左侧展开列 > 首个可见单元格 */
export function resolveDefectTableRowHintAnchor(tr) {
  if (!tr) return null
  return (
    tr.querySelector('.defect-row-kbd-hint-anchor') ||
    tr.querySelector('td.defect-sidebar-expand-body-cell') ||
    tr.querySelector('td:not(.is-hidden) .cell') ||
    tr.querySelector('td .cell') ||
    tr
  )
}
