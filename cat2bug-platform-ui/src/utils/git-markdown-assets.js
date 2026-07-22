/**
 * Resolve / rewrite relative Markdown links & images against a repo file base dir.
 */

/** Transparent 1×1 GIF — valid img src so the browser never hits a custom scheme. */
export const GIT_RAW_PLACEHOLDER_SRC =
  'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw=='

/**
 * @param {string|null|undefined} href
 * @returns {boolean} true when the browser should handle the URL (http, mailto, anchors-only, etc.)
 */
export function isNonRepoMarkdownHref(href) {
  if (href == null) return true
  const raw = String(href).trim()
  if (!raw) return true
  if (raw.startsWith('#')) return true
  if (/^(https?:|mailto:|tel:|data:|javascript:)/i.test(raw)) return true
  if (/^[a-z][a-z0-9+.-]*:/i.test(raw)) return true
  return false
}

/**
 * Resolve a Markdown relative/absolute-in-repo path against the directory of the Markdown file.
 * Returns null when the href is external/special or escapes above the repo root.
 *
 * @param {string} baseDir directory of the markdown file ('' for repo root)
 * @param {string} href
 * @returns {string|null} normalized repo-relative path without leading slash
 */
export function resolveRepoRelativePath(baseDir, href) {
  if (isNonRepoMarkdownHref(href)) return null

  let pathPart = String(href).trim().split('#')[0].split('?')[0]
  if (!pathPart) return null

  try {
    pathPart = decodeURIComponent(pathPart)
  } catch (e) {
    /* keep raw */
  }

  pathPart = pathPart.replace(/\\/g, '/')
  const fromRoot = pathPart.startsWith('/')
  const base = String(baseDir || '').replace(/\\/g, '/').replace(/^\/+|\/+$/g, '')
  const joined = fromRoot
    ? pathPart.replace(/^\/+/, '')
    : (base ? `${base}/${pathPart}` : pathPart)

  const parts = []
  for (const seg of joined.split('/')) {
    if (!seg || seg === '.') continue
    if (seg === '..') {
      if (!parts.length) return null
      parts.pop()
      continue
    }
    parts.push(seg)
  }
  return parts.join('/')
}

/**
 * Parent directory of a file path ('' for root-level files).
 * @param {string} filePath
 * @returns {string}
 */
export function repoFileDir(filePath) {
  const p = String(filePath || '').replace(/\\/g, '/').replace(/^\/+/, '')
  const i = p.lastIndexOf('/')
  return i >= 0 ? p.slice(0, i) : ''
}

/**
 * Encode a repo path into a marker URL that the SPA click handler recognizes.
 * @param {string} path
 * @returns {string}
 */
export function gitMarkdownNavHref(path) {
  return `git-repo://${encodeURIComponent(path)}`
}

/**
 * @param {string} value
 * @returns {string}
 */
function escapeHtmlAttr(value) {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/"/g, '&quot;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

/**
 * Build an &lt;img&gt; that holds the repo path in data-git-raw (hydrated after render).
 * Avoids putting a custom scheme in src (browsers log ERR_UNKNOWN_URL_SCHEME).
 *
 * @param {string} path
 * @param {string} [alt]
 * @param {string} [title]
 * @returns {string}
 */
export function gitMarkdownRawImgHtml(path, alt = '', title = '') {
  const enc = encodeURIComponent(path)
  const titlePart = title != null && title !== '' ? ` title="${escapeHtmlAttr(title)}"` : ''
  return `<img src="${GIT_RAW_PLACEHOLDER_SRC}" data-git-raw="${enc}" alt="${escapeHtmlAttr(alt)}"${titlePart}>`
}

/**
 * @deprecated Prefer {@link gitMarkdownRawImgHtml}; kept for parse round-trip / legacy content.
 * @param {string} path
 * @returns {string}
 */
export function gitMarkdownRawHref(path) {
  return `git-raw://${encodeURIComponent(path)}`
}

/**
 * @param {string|null|undefined} href
 * @returns {string|null}
 */
export function parseGitMarkdownNavHref(href) {
  if (!href || typeof href !== 'string') return null
  const m = /^git-repo:\/\/(.+)$/.exec(href.trim())
  if (!m) return null
  try {
    return decodeURIComponent(m[1])
  } catch (e) {
    return m[1]
  }
}

/**
 * Parse path from legacy git-raw:// src or data-git-raw attribute value.
 * @param {string|null|undefined} hrefOrAttr
 * @returns {string|null}
 */
export function parseGitMarkdownRawHref(hrefOrAttr) {
  if (!hrefOrAttr || typeof hrefOrAttr !== 'string') return null
  const trimmed = hrefOrAttr.trim()
  const legacy = /^git-raw:\/\/(.+)$/.exec(trimmed)
  if (legacy) {
    try {
      return decodeURIComponent(legacy[1])
    } catch (e) {
      return legacy[1]
    }
  }
  // data-git-raw is encodeURIComponent(path)
  try {
    return decodeURIComponent(trimmed)
  } catch (e) {
    return trimmed || null
  }
}

/**
 * @param {Element|null|undefined} img
 * @returns {string|null}
 */
export function parseGitMarkdownRawPathFromImg(img) {
  if (!img || typeof img.getAttribute !== 'function') return null
  const data = img.getAttribute('data-git-raw')
  if (data != null && data !== '') {
    return parseGitMarkdownRawHref(data)
  }
  return parseGitMarkdownRawHref(img.getAttribute('src'))
}

/**
 * Rewrite relative image/link destinations in Markdown source before render.
 *
 * @param {string} markdown
 * @param {{ baseDir: string }} options
 * @returns {string}
 */
export function rewriteGitMarkdownSource(markdown, options) {
  if (!markdown) return ''
  const baseDir = (options && options.baseDir) || ''

  const rewriteLinkDest = (dest) => {
    const resolved = resolveRepoRelativePath(baseDir, dest)
    if (resolved == null) return dest
    return gitMarkdownNavHref(resolved)
  }

  // ![alt](url "title") → HTML img with placeholder src + data-git-raw
  // [text](url "title") → git-repo:// marker (click intercepted; no network fetch)
  let out = String(markdown).replace(
    /(!)?\[([^\]]*)]\(([^)\s]+)(?:\s+"([^"]*)")?\)/g,
    (full, bang, text, dest, title) => {
      if (bang) {
        const resolved = resolveRepoRelativePath(baseDir, dest)
        if (resolved == null) return full
        return gitMarkdownRawImgHtml(resolved, text || '', title || '')
      }
      const next = rewriteLinkDest(dest)
      if (next === dest) return full
      const titlePart = title != null && title !== '' ? ` "${title}"` : ''
      return `[${text}](${next}${titlePart})`
    }
  )

  // HTML <img src> — placeholder + data-git-raw (strip prior data-git-raw to avoid duplicates)
  out = out.replace(
    /<img\b([^>]*)>/gi,
    (full, attrs) => {
      const srcMatch = /\bsrc\s*=\s*(["'])([^"']*)\1/i.exec(attrs)
      if (!srcMatch) return full
      const dest = srcMatch[2]
      const resolved = resolveRepoRelativePath(baseDir, dest)
      if (resolved == null) return full
      let nextAttrs = attrs
        .replace(/\s*data-git-raw\s*=\s*(["'])[^"']*\1/gi, '')
        .replace(/\bsrc\s*=\s*(["'])[^"']*\1/i, `src="${GIT_RAW_PLACEHOLDER_SRC}"`)
      nextAttrs = `${nextAttrs} data-git-raw="${encodeURIComponent(resolved)}"`
      return `<img${nextAttrs}>`
    }
  )
  out = out.replace(
    /(<a\b[^>]*\bhref\s*=\s*)(["'])([^"']+)\2/gi,
    (full, prefix, quote, dest) => {
      const next = rewriteLinkDest(dest)
      return next === dest ? full : `${prefix}${quote}${next}${quote}`
    }
  )
  return out
}
