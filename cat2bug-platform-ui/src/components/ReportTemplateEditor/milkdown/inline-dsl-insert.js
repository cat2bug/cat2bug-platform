import { NodeSelection, TextSelection } from 'prosemirror-state'
import { Fragment } from 'prosemirror-model'
import { resolveTableCellTextSelection } from './table-cell-caret'

/**
 * Collapse NodeSelection / cell selection to a caret TextSelection for inline DSL insert.
 * @param {{ selection: import('prosemirror-state').Selection, doc: import('prosemirror-model').Node }} state
 * @returns {import('prosemirror-state').TextSelection|null} null when selection is already a caret
 */
export function resolveInlineInsertCaret(state) {
  const cellCaret = resolveTableCellTextSelection(state)
  if (cellCaret) {
    return cellCaret
  }

  const { selection, doc } = state
  if (selection instanceof NodeSelection) {
    if (selection.node.isTextblock) {
      return TextSelection.near(doc.resolve(selection.from + 1), 1)
    }
    return TextSelection.near(doc.resolve(selection.to), 1)
  }

  // prosemirror-tables CellSelection
  if (selection && typeof selection.$anchorCell === 'object' && selection.$anchorCell) {
    try {
      return TextSelection.near(selection.$anchorCell, 1)
    } catch (_e) {
      // fall through
    }
  }

  if (selection instanceof TextSelection && !selection.empty) {
    // Insert at the caret/anchor — do not replace the highlighted range for DSL chips.
    return TextSelection.create(doc, selection.from, selection.from)
  }

  return null
}

/**
 * Pull inline Fragment out of a parser doc (usually doc > paragraph > inlines).
 * @param {import('prosemirror-model').Node|null|undefined} parsedDoc
 * @returns {import('prosemirror-model').Fragment|null}
 */
export function extractInlineFragmentFromParsedDoc(parsedDoc) {
  if (!parsedDoc || parsedDoc.childCount < 1) {
    return null
  }
  /** @type {import('prosemirror-model').Fragment[]} */
  const parts = []
  parsedDoc.forEach((child) => {
    if (child.isTextblock && child.content.size > 0) {
      parts.push(child.content)
    }
  })
  if (!parts.length) {
    return null
  }
  let frag = parts[0]
  for (let i = 1; i < parts.length; i += 1) {
    frag = frag.append(parts[i])
  }
  return frag.size > 0 ? frag : null
}

/**
 * @param {import('prosemirror-view').EditorView} view
 * @param {number|null|undefined} preferredPos
 * @returns {import('prosemirror-state').Transaction}
 */
function transactionWithInlineCaret(view, preferredPos) {
  let tr = view.state.tr

  if (preferredPos != null) {
    try {
      const safe = Math.max(0, Math.min(preferredPos, tr.doc.content.size))
      tr = tr.setSelection(TextSelection.near(tr.doc.resolve(safe), 1))
    } catch (_e) {
      // keep current selection
    }
  }

  const collapsed = resolveInlineInsertCaret({
    selection: tr.selection,
    doc: tr.doc
  })
  if (collapsed) {
    tr = tr.setSelection(collapsed)
  }
  return tr
}

/**
 * Insert raw DSL text at a caret (fallback). Never uses a from–to wipe range.
 *
 * @param {import('prosemirror-view').EditorView} view
 * @param {string} content
 * @param {number|null|undefined} preferredPos
 * @returns {{ insertPos: number, insertedLength: number }}
 */
export function insertInlineDslAtCaret(view, content, preferredPos) {
  let tr = transactionWithInlineCaret(view, preferredPos)
  const insertPos = tr.selection.from
  const text = String(content || '')
  tr = tr.insertText(text, insertPos, insertPos)
  view.dispatch(tr.scrollIntoView())
  view.focus()
  return { insertPos, insertedLength: text.length }
}

/**
 * Insert already-parsed inline nodes at the caret (preferred for $image / ${}).
 * Avoids insertText + replaceAll round-trips that can drop sibling cell text.
 *
 * @param {import('prosemirror-view').EditorView} view
 * @param {import('prosemirror-model').Fragment} fragment
 * @param {number|null|undefined} preferredPos
 * @returns {{ insertPos: number, insertedLength: number }}
 */
export function insertInlineFragmentAtCaret(view, fragment, preferredPos) {
  if (!fragment || fragment.size < 1) {
    return { insertPos: view.state.selection.from, insertedLength: 0 }
  }
  let tr = transactionWithInlineCaret(view, preferredPos)
  const insertPos = tr.selection.from
  tr = tr.insert(insertPos, fragment)
  const after = insertPos + fragment.size
  try {
    tr = tr.setSelection(TextSelection.near(tr.doc.resolve(after), 1))
  } catch (_e) {
    // ignore
  }
  view.dispatch(tr.scrollIntoView())
  view.focus()
  return { insertPos, insertedLength: fragment.size }
}

/**
 * Remove an empty paragraph created by block-handle prepare when inline insert used the prior caret.
 *
 * @param {import('prosemirror-view').EditorView} view
 * @param {number|null|undefined} emptyParaPos
 * @param {number} insertPos
 * @param {number} insertedLength
 * @returns {boolean}
 */
export function cleanupEphemeralEmptyParagraph(view, emptyParaPos, insertPos, insertedLength) {
  if (emptyParaPos == null) {
    return false
  }
  let pos = emptyParaPos
  if (insertPos <= pos) {
    pos += insertedLength
  }
  const node = view.state.doc.nodeAt(pos)
  if (!node || node.type.name !== 'paragraph' || node.content.size !== 0) {
    return false
  }
  // Never delete if this is the only block left in a parent that requires content.
  try {
    view.dispatch(view.state.tr.delete(pos, pos + node.nodeSize).scrollIntoView())
    return true
  } catch (_e) {
    return false
  }
}

export { Fragment }
