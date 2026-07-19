import { CellSelection, findTable } from 'prosemirror-tables'

export const TABLE_OPS_OPEN_CLASS = 'is-ops-open'

/**
 * Milkdown table-block clears `.button-group[data-show]` on every pointermove / pointerleave.
 * Drive visibility with our own class so Milkdown attribute thrashing cannot hide the popup.
 * Must be idempotent: only touch DOM when state actually changes (avoids observer/event loops).
 *
 * @param {import('prosemirror-view').EditorView} view
 * @returns {boolean} whether a popup is being kept open
 */
export function syncTableHandlePopup(view) {
  if (!view?.dom) {
    return false
  }

  const { selection } = view.state
  let role = null
  if (selection instanceof CellSelection) {
    if (selection.isColSelection()) {
      role = 'col-drag-handle'
    } else if (selection.isRowSelection()) {
      role = 'row-drag-handle'
    }
  }

  const block = role ? findTableBlockEl(view, selection) : null
  const targetHandle = role
    ? findHandle(block || view.dom, role)
    : null

  clearOpsOpenExcept(view.dom, targetHandle)

  if (!role || !targetHandle) {
    return false
  }
  return forceHandlePopup(targetHandle)
}

/**
 * @param {import('prosemirror-view').EditorView} view
 * @param {import('prosemirror-tables').CellSelection} selection
 * @returns {HTMLElement|null}
 */
function findTableBlockEl(view, selection) {
  const table = findTable(selection.$anchorCell)
  if (!table) {
    return null
  }
  const nodeDom = view.nodeDOM(table.pos)
  if (!nodeDom) {
    return null
  }
  if (typeof nodeDom.closest === 'function') {
    return nodeDom.closest('.milkdown-table-block') || nodeDom
  }
  return null
}

/**
 * @param {ParentNode} root
 * @param {'col-drag-handle'|'row-drag-handle'} role
 * @returns {HTMLElement|null}
 */
function findHandle(root, role) {
  const visible = root.querySelector(
    `[data-role="${role}"][data-show="true"]`
  )
  return visible || root.querySelector(`[data-role="${role}"]`)
}

/**
 * @param {ParentNode} root
 * @param {Element|null} keep
 */
export function clearOpsOpenExcept(root, keep) {
  root.querySelectorAll?.(`.milkdown-table-block .cell-handle.${TABLE_OPS_OPEN_CLASS}`).forEach((el) => {
    if (el === keep) {
      return
    }
    el.classList.remove(TABLE_OPS_OPEN_CLASS)
  })
}

/**
 * @param {ParentNode} root
 * @deprecated use clearOpsOpenExcept
 */
export function clearOpsOpen(root) {
  clearOpsOpenExcept(root, null)
}

/**
 * @param {HTMLElement} handle
 */
function forceHandlePopup(handle) {
  if (handle.dataset.show !== 'true') {
    handle.dataset.show = 'true'
  }
  if (!handle.classList.contains(TABLE_OPS_OPEN_CLASS)) {
    handle.classList.add(TABLE_OPS_OPEN_CLASS)
  }

  const group = handle.querySelector('.button-group')
  if (!group) {
    return false
  }
  if (group.dataset.show !== 'true') {
    group.dataset.show = 'true'
  }
  return true
}

/**
 * @param {import('prosemirror-state').Selection} selection
 */
export function isTableColOrRowSelection(selection) {
  return (
    selection instanceof CellSelection &&
    (selection.isColSelection() || selection.isRowSelection())
  )
}
