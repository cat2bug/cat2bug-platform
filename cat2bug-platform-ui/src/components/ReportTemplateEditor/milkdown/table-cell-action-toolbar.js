import { isSelectionInsideTableCell } from './table-cell-caret'
import {
  getMergeToolbarState,
  runMergeCells,
  runSplitCell
} from './table-cell-merge'

export const CELL_ACTION_TOOLBAR_CLASS = 'cat2bug-table-cell-action-toolbar'

const TABLE_CELL_TYPES = new Set(['table_cell', 'table_header'])

/** Crepe-toolbar sized 24×24 icons (currentColor). */
const ICON_ADD =
  '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/></svg>'
const ICON_MERGE =
  '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M5 5h6v2H7v10h4v2H5V5zm14 0v14h-6v-2h4V7h-4V5h6zM9.5 11h5v2h-5v-2z"/></svg>'
const ICON_SPLIT =
  '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M5 5h14v2H5V5zm0 12h14v2H5v-2zm0-6h6v2H5v-2zm8 0h6v2h-6v-2zM11 7h2v10h-2V7z"/></svg>'

/**
 * @param {string} svg
 * @param {string} label
 * @returns {HTMLButtonElement}
 */
function createIconButton(svg, label) {
  const btn = document.createElement('button')
  btn.type = 'button'
  btn.className = `${CELL_ACTION_TOOLBAR_CLASS}__btn toolbar-item`
  btn.setAttribute('aria-label', label)
  btn.title = label
  btn.dataset.action = label
  btn.innerHTML = svg
  return btn
}

/**
 * Duck-type CellSelection — avoid `instanceof` across duplicate prosemirror copies.
 * @param {import('prosemirror-state').Selection} selection
 * @returns {boolean}
 */
export function isCellSelectionLike(selection) {
  return typeof selection?.forEachCell === 'function'
}

/**
 * Non-empty inline text range (Crepe format toolbar territory).
 * Avoids `instanceof TextSelection` which fails when Milkdown and app resolve
 * different copies of prosemirror-state.
 *
 * @param {import('prosemirror-state').EditorState} state
 * @returns {boolean}
 */
export function isNonEmptyInlineTextSelection(state) {
  const { selection, doc } = state
  if (!selection || selection.empty || isCellSelectionLike(selection)) {
    return false
  }
  try {
    return doc.textBetween(selection.from, selection.to, '\n', '\0').length > 0
  } catch (_e) {
    return selection.to > selection.from
  }
}

/**
 * Absolute doc position of the table cell containing `$pos`, or null.
 * @param {import('prosemirror-model').ResolvedPos} $pos
 * @returns {number|null}
 */
export function findTableCellPos($pos) {
  for (let depth = $pos.depth; depth > 0; depth -= 1) {
    if (TABLE_CELL_TYPES.has($pos.node(depth).type.name)) {
      return $pos.before(depth)
    }
  }
  return null
}

/**
 * Top-most (then left-most) cell position for the current selection.
 * @param {import('prosemirror-state').EditorState} state
 * @returns {number|null}
 */
export function resolveTopCellPos(state) {
  const { selection } = state
  if (isCellSelectionLike(selection)) {
    /** @type {number|null} */
    let bestPos = null
    selection.forEachCell((_node, pos) => {
      if (bestPos == null || pos < bestPos) {
        bestPos = pos
      }
    })
    return bestPos
  }
  if (isSelectionInsideTableCell(selection)) {
    return findTableCellPos(selection.$from)
  }
  return null
}

/**
 * Resolve the top-most selected/edited cell DOM element.
 * @param {import('prosemirror-view').EditorView} view
 * @returns {HTMLElement|null}
 */
export function resolveTopCellDom(view) {
  const { selection } = view.state
  if (isCellSelectionLike(selection)) {
    /** @type {HTMLElement|null} */
    let best = null
    let bestTop = Infinity
    let bestLeft = Infinity
    selection.forEachCell((_node, pos) => {
      const dom = view.nodeDOM(pos)
      if (!(dom instanceof HTMLElement)) {
        return
      }
      const rect = dom.getBoundingClientRect()
      if (rect.top < bestTop - 0.5 || (Math.abs(rect.top - bestTop) <= 0.5 && rect.left < bestLeft)) {
        best = dom
        bestTop = rect.top
        bestLeft = rect.left
      }
    })
    return best
  }
  const cellPos = findTableCellPos(selection.$from)
  if (cellPos == null) {
    return null
  }
  const dom = view.nodeDOM(cellPos)
  return dom instanceof HTMLElement ? dom : null
}

/**
 * Unified floating toolbar: 添加 / 合并 / 拆分 on the topmost cell.
 * Styled like Crepe `.milkdown-toolbar` (mounted under `.milkdown` for tokens).
 *
 * @param {{
 *   getView: () => import('prosemirror-view').EditorView | null | undefined,
 *   onAdd: (anchorRect: DOMRect) => void,
 *   isDark?: () => boolean
 * }} options
 * @returns {() => void} uninstall
 */
export function installTableCellActionToolbar(options) {
  const { getView, onAdd, isDark = () => false } = options

  const el = document.createElement('div')
  el.className = `${CELL_ACTION_TOOLBAR_CLASS} milkdown-toolbar`
  el.setAttribute('contenteditable', 'false')
  el.setAttribute('data-show', 'false')
  el.style.display = 'none'

  const addBtn = createIconButton(ICON_ADD, '添加')
  const mergeBtn = createIconButton(ICON_MERGE, '合并')
  const splitBtn = createIconButton(ICON_SPLIT, '拆分')

  el.append(addBtn, mergeBtn, splitBtn)

  let raf = 0
  let visible = false
  /** True after the user clicks or types in the editor (not drawer autofocus). */
  let userActivated = false
  /** @type {HTMLElement | null} */
  let mountedHost = null

  function ensureMounted(view) {
    const host =
      (view.dom.closest('.milkdown') instanceof HTMLElement
        ? view.dom.closest('.milkdown')
        : null) ||
      (view.dom.parentElement instanceof HTMLElement ? view.dom.parentElement : null) ||
      document.body
    if (mountedHost !== host) {
      host.appendChild(el)
      mountedHost = host
    }
  }

  function hide() {
    el.setAttribute('data-show', 'false')
    el.style.display = 'none'
    visible = false
  }

  function onAddDown(event) {
    event.preventDefault()
    event.stopPropagation()
    const view = getView()
    view?.focus()
    const rect = addBtn.getBoundingClientRect()
    onAdd(rect)
  }

  function onMergeDown(event) {
    event.preventDefault()
    event.stopPropagation()
    const view = getView()
    if (!view) {
      return
    }
    view.focus()
    runMergeCells(view.state, view.dispatch.bind(view))
  }

  function onSplitDown(event) {
    event.preventDefault()
    event.stopPropagation()
    const view = getView()
    if (!view) {
      return
    }
    view.focus()
    runSplitCell(view.state, view.dispatch.bind(view))
  }

  addBtn.addEventListener('mousedown', onAddDown)
  mergeBtn.addEventListener('mousedown', onMergeDown)
  splitBtn.addEventListener('mousedown', onSplitDown)

  function sync() {
    raf = 0
    const view = getView()
    if (!view?.dom) {
      hide()
      return
    }

    ensureMounted(view)

    // Hide until the user actually clicks/types in the body — drawer open may
    // autofocus the editor with the caret already inside a table cell.
    if (!userActivated || !view.hasFocus()) {
      hide()
      return
    }

    // Selecting cell text → only Crepe format toolbar.
    if (isNonEmptyInlineTextSelection(view.state)) {
      hide()
      return
    }

    const cellDom = resolveTopCellDom(view)
    if (!cellDom) {
      hide()
      return
    }

    const mergeState = getMergeToolbarState(view.state)
    mergeBtn.disabled = !mergeState.canMerge
    mergeBtn.hidden = false
    splitBtn.disabled = !mergeState.canSplit
    splitBtn.hidden = false

    const rect = cellDom.getBoundingClientRect()
    el.classList.toggle('is-dark', !!isDark())
    el.setAttribute('data-show', 'true')
    el.style.display = 'flex'
    const left = Math.max(4, rect.left + rect.width / 2)
    const top = Math.max(4, rect.top - 44)
    el.style.left = `${left}px`
    el.style.top = `${top}px`
    visible = true
  }

  function scheduleSync() {
    if (raf) {
      return
    }
    raf = requestAnimationFrame(sync)
  }

  function onUserActivate() {
    userActivated = true
    scheduleSync()
  }

  const onScroll = () => scheduleSync()
  window.addEventListener('scroll', onScroll, true)
  window.addEventListener('resize', onScroll)

  /** @type {HTMLElement | null} */
  let boundDom = null

  function bindViewEvents(view) {
    if (!view?.dom || boundDom === view.dom) {
      return
    }
    if (boundDom) {
      boundDom.removeEventListener('keyup', onUserActivate)
      boundDom.removeEventListener('keydown', onUserActivate)
      boundDom.removeEventListener('mouseup', scheduleSync)
      boundDom.removeEventListener('mousedown', onUserActivate)
      boundDom.removeEventListener('focusin', scheduleSync)
      boundDom.removeEventListener('focusout', scheduleSync)
    }
    boundDom = view.dom
    // Do NOT MutationObserver the editor: hover chrome / toolbar style writes
    // would re-enter sync and stall mouseenter (variable hover chrome dies).
    boundDom.addEventListener('keyup', onUserActivate)
    boundDom.addEventListener('keydown', onUserActivate)
    boundDom.addEventListener('mouseup', scheduleSync)
    boundDom.addEventListener('mousedown', onUserActivate)
    boundDom.addEventListener('focusin', scheduleSync)
    boundDom.addEventListener('focusout', scheduleSync)
  }

  const onDocSelectionChange = () => {
    if (userActivated) {
      scheduleSync()
    }
  }
  document.addEventListener('selectionchange', onDocSelectionChange)

  const intervalId = window.setInterval(() => {
    const current = getView()
    if (current?.dom) {
      bindViewEvents(current)
    }
    if (userActivated && (current?.hasFocus() || visible)) {
      scheduleSync()
    }
  }, 200)

  const initial = getView()
  if (initial?.dom) {
    ensureMounted(initial)
    bindViewEvents(initial)
  }
  // Keep hidden until the user interacts with the editor body.
  hide()

  return () => {
    window.clearInterval(intervalId)
    if (raf) {
      cancelAnimationFrame(raf)
      raf = 0
    }
    window.removeEventListener('scroll', onScroll, true)
    window.removeEventListener('resize', onScroll)
    document.removeEventListener('selectionchange', onDocSelectionChange)
    boundDom?.removeEventListener('keyup', onUserActivate)
    boundDom?.removeEventListener('keydown', onUserActivate)
    boundDom?.removeEventListener('mouseup', scheduleSync)
    boundDom?.removeEventListener('mousedown', onUserActivate)
    boundDom?.removeEventListener('focusin', scheduleSync)
    boundDom?.removeEventListener('focusout', scheduleSync)
    addBtn.removeEventListener('mousedown', onAddDown)
    mergeBtn.removeEventListener('mousedown', onMergeDown)
    splitBtn.removeEventListener('mousedown', onSplitDown)
    el.remove()
  }
}
