import { normalizeKey } from '@/plugins/shortcut/keymap'
import { ROW_KBD_RESERVED } from '@/plugins/shortcut/reserved-keys'

export { ROW_KBD_RESERVED }

/** page-action-hints 浮层 id（与 mixin 内 OVERLAY_ID 一致） */
export const PAGE_KBD_OVERLAY_ID = 'cat2bug-page-kbd-overlay'

const LETTER_POOL = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('')

/** 表格首列行徽标默认浮层参数（有文字/无文字时由 resolveTableRowFirstColumnKbdBadgeLayout 覆盖 placement） */
export const TABLE_ROW_KBD_FLOAT_TEXT_BOTTOM_RIGHT = {
  placement: 'bottom-right-outset',
  outset: 2
}

const ROW_KBD_HINT_ANCHOR_SELECTORS = [
  '.defect-row-kbd-hint-anchor',
  '.case-row-kbd-hint-anchor',
  '.plan-row-kbd-hint-anchor',
  '.module-row-kbd-hint-anchor',
  '.report-row-kbd-hint-anchor',
  '.ai-account-row-kbd-hint-anchor',
  '.defect-field-row-kbd-hint-anchor',
  '.project-row-kbd-hint-anchor'
].join(', ')

const ROW_KBD_ROW_KEY_ATTRS = ['data-plan-id', 'data-case-id', 'data-defect-id', 'data-report-id']

function escapeAttrSelectorValue(value) {
  return String(value).replace(/\\/g, '\\\\').replace(/"/g, '\\"')
}

/** Cat2BugTable 外层 wrap 或 el-table 根节点 → `.el-table` */
export function resolveElTableRoot(tableRoot) {
  if (!tableRoot) return null
  if (tableRoot.classList && tableRoot.classList.contains('el-table')) return tableRoot
  const inner = tableRoot.querySelector && tableRoot.querySelector('.el-table')
  return inner || tableRoot
}

/**
 * 主表体行 → 左侧固定列对应行（行数据 field 匹配 > data-* > 行号）。
 * @param {{ attr?: string, field?: string, value: string|number }} [rowKey]
 */
export function findFixedTableRow(tr, tableRoot, anchorSelector, rowKey) {
  const root = resolveElTableRoot(tableRoot)
  if (!root || !tr) return null
  const fixedRoot = root.querySelector('.el-table__fixed')
  if (!fixedRoot) return null

  const sel = anchorSelector || ROW_KBD_HINT_ANCHOR_SELECTORS
  const fixedBody = fixedRoot.querySelector('.el-table__fixed-body-wrapper tbody')

  const matchByRowData = () => {
    if (!fixedBody || !rowKey || rowKey.field == null || rowKey.value == null || rowKey.value === '') {
      return null
    }
    const want = String(rowKey.value)
    for (let i = 0; i < fixedBody.children.length; i++) {
      const fixedTr = fixedBody.children[i]
      const data = resolveElTableRowData(fixedTr)
      if (data && String(data[rowKey.field]) === want) return fixedTr
    }
    return null
  }

  const matchByAttr = (attr, value) => {
    if (attr == null || value == null || value === '') return null
    const scoped = fixedBody
      ? fixedBody.querySelector(`[${attr}="${escapeAttrSelectorValue(value)}"]`)
      : fixedRoot.querySelector(`[${attr}="${escapeAttrSelectorValue(value)}"]`)
    return scoped ? scoped.closest('tr') : null
  }

  const byRowData = matchByRowData()
  if (byRowData) return byRowData

  if (rowKey && rowKey.attr) {
    const byKey = matchByAttr(rowKey.attr, rowKey.value)
    if (byKey) return byKey
  }

  const mainAnchor = tr.querySelector(sel)
  if (mainAnchor) {
    for (let i = 0; i < ROW_KBD_ROW_KEY_ATTRS.length; i++) {
      const attr = ROW_KBD_ROW_KEY_ATTRS[i]
      const val = mainAnchor.getAttribute(attr)
      if (val != null && val !== '') {
        const byAttr = matchByAttr(attr, val)
        if (byAttr) return byAttr
      }
    }
  }

  const tbody = tr.parentElement
  const rowIndex = tbody ? Array.from(tbody.children).indexOf(tr) : -1
  if (fixedBody && rowIndex >= 0 && fixedBody.children[rowIndex]) {
    return fixedBody.children[rowIndex]
  }
  return null
}

/** 行内第一个可见 td（跳过 is-hidden / 零宽列） */
export function resolveTableRowFirstVisibleTd(tr) {
  if (!tr) return null
  const tds = tr.querySelectorAll('td.el-table__cell, td')
  for (let i = 0; i < tds.length; i++) {
    const td = tds[i]
    if (td.classList.contains('is-hidden')) continue
    const rect = td.getBoundingClientRect()
    if (rect.width < 1 || rect.height < 1) continue
    return td
  }
  return tr.querySelector('td:first-child')
}

/** 行内第一个可见单元格内容区（.cell） */
export function resolveTableRowFirstVisibleCell(tr) {
  const td = resolveTableRowFirstVisibleTd(tr)
  if (!td) return null
  return td.querySelector('.cell') || td
}

/**
 * 表格行快捷键锚点：固定列行 / 主表体行的**第一个可见列**。
 * @param {{ attr?: string, field?: string, value: string|number }} [rowKey]
 */
export function resolveTableRowFirstColumnHintAnchor(tr, tableRoot, anchorSelector, rowKey) {
  const root = resolveElTableRoot(tableRoot)

  const fixedTr = findFixedTableRow(tr, root, anchorSelector, rowKey)
  if (fixedTr) {
    const cell = resolveTableRowFirstVisibleCell(fixedTr)
    if (cell) return cell
  }

  const cell = resolveTableRowFirstVisibleCell(tr)
  if (cell) return cell

  const sel = anchorSelector || ROW_KBD_HINT_ANCHOR_SELECTORS
  const inRow = tr && tr.querySelector(sel)
  if (inRow) return inRow
  return resolveDefectTableRowHintAnchor(tr)
}

/** 首列单元格是否有可见文字（非空白） */
export function hasTableCellVisibleText(el) {
  if (!el) return false
  return String(el.textContent || '').trim().length > 0
}

/**
 * 表格首列行徽标布局：有文字 → 文字右下角；内容为空 → 单元格居中。
 * @returns {{ rect: DOMRect|object|null, floatOffset: { placement: string, outset?: number } }}
 */
export function resolveTableRowFirstColumnKbdBadgeLayout(tr, tableRoot, anchorSelector, rowKey) {
  const root = resolveElTableRoot(tableRoot)
  const colRect = resolveDefectTableRowHintPositionRect(tr, root, anchorSelector, rowKey)
  const anchor = resolveTableRowFirstColumnHintAnchor(tr, root, anchorSelector, rowKey)

  let cellRect = colRect
  if ((!cellRect || cellRect.width <= 0 || cellRect.height <= 0) && anchor) {
    cellRect = anchor.getBoundingClientRect()
  }

  let textRect = null
  if (anchor && hasTableCellVisibleText(anchor)) {
    textRect = getElementTextRect(anchor)
  }

  if (textRect && textRect.width > 0 && textRect.height > 0) {
    return {
      rect: textRect,
      floatOffset: { placement: 'bottom-right-outset', outset: 2 }
    }
  }

  return {
    rect: cellRect,
    floatOffset: { placement: 'center-cell', outset: 0 }
  }
}

/** @deprecated 请用 resolveTableRowFirstColumnKbdBadgeLayout */
export function resolveTableRowFirstColumnHintTextRect(tr, tableRoot, anchorSelector, rowKey) {
  return resolveTableRowFirstColumnKbdBadgeLayout(tr, tableRoot, anchorSelector, rowKey).rect
}

/** ⌘/Ctrl + 单键：归一化字母或数字（兼容 Digit1 / Numpad1） */
export function resolvePageActionLetter(e) {
  if (!e) return ''
  const k = e.key
  if (k && k.length === 1) {
    if (/^\d$/.test(k)) return k
    if (/[a-zA-Z]/.test(k)) return k.toUpperCase()
  }
  if (e.code && /^Digit[0-9]$/.test(e.code)) {
    return e.code.slice(5)
  }
  if (e.code && /^Numpad[0-9]$/.test(e.code)) {
    return e.code.slice(6)
  }
  return ''
}

/** 列表行 ⌘/Ctrl 数字徽标是否正在展示（用于避免与布局导航数字键冲突） */
export function hasActivePageRowKbdHints() {
  const overlay = document.getElementById(PAGE_KBD_OVERLAY_ID)
  if (!overlay) return false
  return overlay.querySelector('.defect-row-kbd-hint-float') != null
}

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

/** ⌘/Ctrl + 方向键：滚动表格主表体（垂直/水平） */
export function scrollTableBodyByArrow(bodyWrap, key) {
  if (!bodyWrap || !key) return false
  const stepY = Math.max(48, Math.round(bodyWrap.clientHeight * 0.35))
  const stepX = Math.max(80, Math.round(bodyWrap.clientWidth * 0.25))
  if (key === 'ArrowUp') {
    bodyWrap.scrollTop = Math.max(0, bodyWrap.scrollTop - stepY)
    return true
  }
  if (key === 'ArrowDown') {
    bodyWrap.scrollTop = Math.min(
      bodyWrap.scrollHeight - bodyWrap.clientHeight,
      bodyWrap.scrollTop + stepY
    )
    return true
  }
  if (key === 'ArrowLeft') {
    bodyWrap.scrollLeft = Math.max(0, bodyWrap.scrollLeft - stepX)
    return true
  }
  if (key === 'ArrowRight') {
    bodyWrap.scrollLeft = Math.min(
      bodyWrap.scrollWidth - bodyWrap.clientWidth,
      bodyWrap.scrollLeft + stepX
    )
    return true
  }
  return false
}

/** 通知列表：首列勾选框右下角 */
export function resolveNoticeTableCheckboxAnchor(tr) {
  if (!tr) return null
  return (
    tr.querySelector('td.el-table-column--selection .el-checkbox') ||
    tr.querySelector('td.el-table-column--selection .cell') ||
    tr.querySelector('td.el-table-column--selection')
  )
}

/** 从 Element UI 表格行 DOM 解析行数据（含树表可见行） */
export function resolveElTableRowData(tr) {
  if (!tr) return null
  let vm = tr.__vue__
  while (vm) {
    if (vm.row != null && typeof vm.row === 'object') return vm.row
    vm = vm.$parent
  }
  return null
}

/** 表格行快捷键徽标锚点：编号列 > 左侧展开列 > 首个可见单元格 */
export function resolveDefectTableRowHintAnchor(tr) {
  if (!tr) return null
  return (
    tr.querySelector('.defect-row-kbd-hint-anchor') ||
    tr.querySelector('.case-row-kbd-hint-anchor') ||
    tr.querySelector('.plan-row-kbd-hint-anchor') ||
    tr.querySelector('.module-row-kbd-hint-anchor') ||
    tr.querySelector('.report-row-kbd-hint-anchor') ||
    tr.querySelector('.ai-account-row-kbd-hint-anchor') ||
    tr.querySelector('td.defect-sidebar-expand-body-cell') ||
    tr.querySelector('td:not(.is-hidden) .cell') ||
    tr.querySelector('td .cell') ||
    tr
  )
}

/** 主表首列宽度（px），用于横向滚动时固定行徽标水平位置 */
export function getDefectTableFirstColumnWidth(tableRoot) {
  if (!tableRoot || !tableRoot.querySelector) return 90
  const fixedHeader = tableRoot.querySelector('.el-table__fixed .el-table__fixed-header-wrapper thead tr')
  if (fixedHeader) {
    const th = fixedHeader.querySelector('th:first-child')
    if (th) {
      const w = th.getBoundingClientRect().width
      if (w > 0) return w
    }
  }
  const mainHeader =
    tableRoot.querySelector('.el-table__header-wrapper:not(.el-table__fixed-header-wrapper) thead tr') ||
    tableRoot.querySelector('.el-table__header-wrapper thead tr')
  if (mainHeader) {
    const th = mainHeader.querySelector('th:first-child')
    if (th) {
      const w = th.getBoundingClientRect().width
      if (w > 0) return w
    }
  }
  return 90
}

/**
 * 行徽标定位矩形：纵坐标跟行，横坐标钉在首列（左侧 fixed 列或表体左缘），不随水平滚动漂移。
 */
export function resolveDefectTableRowHintPositionRect(tr, tableRoot, anchorSelector, rowKey) {
  if (!tr || typeof tr.getBoundingClientRect !== 'function') return null
  const rowRect = tr.getBoundingClientRect()
  if (rowRect.height <= 0) return null

  const root = resolveElTableRoot(tableRoot)
  const fixedTr = findFixedTableRow(tr, root, anchorSelector, rowKey)
  if (fixedTr) {
    const firstTd = resolveTableRowFirstVisibleTd(fixedTr)
    if (firstTd) {
      const colRect = firstTd.getBoundingClientRect()
      if (colRect.width > 0) {
        return {
          top: rowRect.top,
          bottom: rowRect.bottom,
          left: colRect.left,
          right: colRect.right,
          width: colRect.width,
          height: rowRect.height
        }
      }
    }
  }

  const bodyWrap = root ? getDefectTableScrollBody(root) : tr.closest('.el-table__body-wrapper')
  if (bodyWrap) {
    const wrapRect = bodyWrap.getBoundingClientRect()
    const colWidth = getDefectTableFirstColumnWidth(root)
    return {
      top: rowRect.top,
      bottom: rowRect.bottom,
      left: wrapRect.left,
      right: wrapRect.left + colWidth,
      width: colWidth,
      height: rowRect.height
    }
  }

  const firstTd = resolveTableRowFirstVisibleTd(tr)
  if (firstTd) {
    const colRect = firstTd.getBoundingClientRect()
    return {
      top: rowRect.top,
      bottom: rowRect.bottom,
      left: colRect.left,
      right: colRect.right,
      width: colRect.width,
      height: rowRect.height
    }
  }
  return rowRect
}
