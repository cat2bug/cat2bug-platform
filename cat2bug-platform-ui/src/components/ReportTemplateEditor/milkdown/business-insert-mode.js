import { isSelectionInsideTableCell } from './table-cell-caret'

/**
 * True for `$image` / `$img` — inline atoms that insert at the caret.
 * @param {string} content
 * @returns {boolean}
 */
export function isInlineImageDslContent(content) {
  return /^\s*\$(?:image|img)\b/.test(String(content || ''))
}

/**
 * Milkdown `insert(md, inline)` — the 2nd arg is **inline**, not a flags object.
 * Block insert (`inline=false`) replaces with doc-level blocks and can lift
 * content out of a table cell (one table → paragraph + table).
 *
 * Image DSL always uses caret insert so it never replaces the rest of the line.
 *
 * @param {import('prosemirror-state').Selection} selection
 * @param {string} [content]
 * @returns {'inline-text' | 'block' | 'block-newline'}
 */
export function resolveBusinessInsertMode(selection, content) {
  if (isInlineImageDslContent(content) || isSelectionInsideTableCell(selection)) {
    return 'inline-text'
  }
  const parent = selection.$from.parent
  if (parent.isTextblock && parent.content.size === 0) {
    return 'block'
  }
  return 'block-newline'
}
