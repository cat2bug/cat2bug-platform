import { resolveProjectIconUrl, resolveUploadUrl } from '@/utils/upload-asset'
import { parseProjectIconIndex } from '@/utils/project-default-icons'

/**
 * Resolve a materialized report image src for the current frontend origin.
 * @param {string} src
 * @returns {string}
 */
export function resolveStaticReportImageSrc(src) {
  const raw = String(src || '').trim()
  if (!raw) {
    return ''
  }
  if (parseProjectIconIndex(raw) != null) {
    return resolveProjectIconUrl(raw)
  }
  return resolveUploadUrl(raw)
}

/**
 * Rewrite `<img src="/static/img/...">` in report markdown/HTML so Vite-dev
 * loads backend static icons via the API proxy (or vite asset URLs for presets).
 * @param {string} markdown
 * @returns {string}
 */
export function rewriteReportSnapshotImageSrcs(markdown) {
  const text = markdown == null ? '' : String(markdown)
  if (!text || text.indexOf('<img') === -1) {
    return text
  }
  return text.replace(/<img\b([^>]*?)\/?>/gi, (full, attrs) => {
    const srcMatch = /\bsrc\s*=\s*(["'])(.*?)\1/i.exec(attrs || '')
    if (!srcMatch) {
      return full
    }
    const raw = srcMatch[2]
    const next = resolveStaticReportImageSrc(raw)
    if (!next || next === raw) {
      return full
    }
    const nextAttrs = String(attrs || '').replace(srcMatch[0], `src=${srcMatch[1]}${next}${srcMatch[1]}`)
    return full.replace(attrs, nextAttrs)
  })
}
