/**
 * Bridge so the left-+ overlay and top toolbar share one link-insert dialog.
 * The Vue toolbar registers a requester; overlay items call it when inserting a link.
 */

/** @type {null | (() => Promise<string|null>)} */
let linkInsertRequester = null

/**
 * @param {null | (() => Promise<string|null>)} requester
 */
export function setLinkInsertRequester(requester) {
  linkInsertRequester = typeof requester === 'function' ? requester : null
}

/**
 * Open the link dialog (if registered) and resolve to markdown `[text](url)`, or null if cancelled.
 * @returns {Promise<string|null>}
 */
export function requestLinkInsertMarkdown() {
  if (typeof linkInsertRequester !== 'function') {
    return Promise.resolve(null)
  }
  return Promise.resolve(linkInsertRequester())
}

/**
 * @param {string} describe
 * @param {string} url
 * @returns {string}
 */
export function formatMarkdownLink(describe, url) {
  const text = String(describe || '').trim() || String(url || '').trim() || 'link'
  const href = String(url || '').trim()
  return `[${text}](${href})`
}
