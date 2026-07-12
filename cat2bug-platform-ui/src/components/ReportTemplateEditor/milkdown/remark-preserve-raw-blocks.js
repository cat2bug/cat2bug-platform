import { fromMarkdown } from 'mdast-util-from-markdown'
import { gfmFromMarkdown } from 'mdast-util-gfm'
import { normalizeDslSource } from '../dsl/normalize'
import { splitSourceWithRawBlocks } from '../dsl/complex-blocks'

function parseMarkdownChunk(chunk) {
  return fromMarkdown(chunk, {
    extensions: [gfmFromMarkdown()],
    mdastExtensions: [gfmFromMarkdown()]
  })
}

/**
 * Split source into preserved raw blocks (plan mega-table / multimd merge) before DSL promotion.
 */
export function remarkPreserveRawBlocks() {
  return (tree, file) => {
    const source = normalizeDslSource(String(file.value || ''))
    const parts = splitSourceWithRawBlocks(source)
    if (!parts.some((part) => part.type === 'raw')) {
      return
    }

    /** @type {import('mdast').Root['children']} */
    const children = []

    for (const part of parts) {
      if (part.type === 'raw') {
        children.push({
          type: 'cat2bugRawBlock',
          kind: part.kind,
          value: part.value
        })
        continue
      }
      if (!part.value.trim()) {
        continue
      }
      const sub = parseMarkdownChunk(part.value)
      if (sub.children?.length) {
        children.push(...sub.children)
      }
    }

    tree.children = children
  }
}
