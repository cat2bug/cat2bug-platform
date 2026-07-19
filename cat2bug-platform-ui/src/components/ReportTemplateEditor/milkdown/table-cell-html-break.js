/**
 * Convert inline html nodes that are only `<br>` into hardbreak inside table cells.
 * Milkdown/remark parses GFM cell `<br>` as html atoms (shown as literal text);
 * Mod-Enter inserts hardbreak which we serialize back as `<br>`.
 *
 * @param {import('@milkdown/prose/state').EditorState} state
 * @returns {import('@milkdown/prose/state').Transaction|null}
 */
export function buildConvertTableHtmlBreakTransaction(state) {
  const hardbreakType = state.schema.nodes.hardbreak
  const htmlType = state.schema.nodes.html
  if (!hardbreakType || !htmlType) {
    return null
  }

  /** @type {{ from: number, to: number }[]} */
  const replacements = []
  state.doc.descendants((node, pos) => {
    if (node.type !== htmlType) {
      return true
    }
    const value = String(node.attrs.value || '').trim()
    if (!/^<br\s*\/?\s*>$/i.test(value)) {
      return false
    }
    const $pos = state.doc.resolve(pos)
    let inTable = false
    for (let d = $pos.depth; d > 0; d -= 1) {
      if ($pos.node(d).type.spec.tableRole === 'table') {
        inTable = true
        break
      }
    }
    if (inTable) {
      replacements.push({ from: pos, to: pos + node.nodeSize })
    }
    return false
  })

  if (!replacements.length) {
    return null
  }

  let tr = state.tr
  for (let i = replacements.length - 1; i >= 0; i -= 1) {
    const { from, to } = replacements[i]
    tr = tr.replaceWith(from, to, hardbreakType.create())
  }
  return tr
}
