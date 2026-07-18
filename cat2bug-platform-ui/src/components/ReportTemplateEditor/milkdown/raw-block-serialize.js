/**
 * Serialize a preserved multi-line raw block (plan statistics / multimd) without
 * mdast hard-breaks. Using `break` between lines becomes `\\\n` in CommonMark and
 * accumulates backslashes on every Milkdown ↔ source sync.
 * `html` nodes are written verbatim by mdast-util-to-markdown.
 *
 * @param {{ addNode: (type: string, children?: unknown, value?: string) => unknown }} state
 * @param {string} value
 */
export function emitPreservedRawMarkdown(state, value) {
  const raw = value || ''
  if (!raw) {
    return
  }
  state.addNode('html', undefined, raw)
}
