import { normalizeDslSource } from '../dsl/normalize'

/** Plain-text nodes still carrying DSL tokens should trigger a forced Milkdown resync. */
const RAW_DSL_IN_TEXT_RE = /\$(?:table|card|image|img)\{|\$\{[^}]+\}/

/**
 * @param {string} text
 * @returns {boolean}
 */
export function textContainsRawDslTokens(text) {
  if (!text) {
    return false
  }
  return RAW_DSL_IN_TEXT_RE.test(normalizeDslSource(text))
}

/**
 * @param {import('@milkdown/ctx').Ctx} ctx
 * @param {symbol} editorViewCtxKey
 * @returns {boolean}
 */
export function documentContainsRawDslText(ctx, editorViewCtxKey) {
  const view = ctx.get(editorViewCtxKey)
  if (!view) {
    return false
  }

  let found = false
  view.state.doc.descendants((node) => {
    if (found) {
      return false
    }
    if (!node.isText || !node.text) {
      return
    }
    if (textContainsRawDslTokens(node.text)) {
      found = true
    }
  })
  return found
}
