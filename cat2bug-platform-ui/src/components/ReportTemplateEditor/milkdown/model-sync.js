import { normalizeDslSource } from '../dsl/normalize'

/**
 * Whether parent modelValue should replace the in-memory Milkdown document.
 * While the visual editor is focused it is the source of truth (avoids
 * trailing empty paragraphs / cursor jumps from split-pane echo).
 *
 * @param {{ force?: boolean, editorFocused?: boolean }} options
 * @returns {boolean}
 */
export function shouldApplyExternalModelSync({ force = false, editorFocused = false } = {}) {
  if (force) {
    return true
  }
  return !editorFocused
}

/**
 * Compare markdown for skip-sync decisions; trailing newline-only diffs are ignored.
 * @param {string} a
 * @param {string} b
 * @returns {boolean}
 */
export function markdownSemanticallyEqual(a, b) {
  const left = normalizeDslSource(a || '')
  const right = normalizeDslSource(b || '')
  if (left === right) {
    return true
  }
  return left.replace(/\n+$/, '') === right.replace(/\n+$/, '')
}
