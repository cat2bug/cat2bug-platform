/** Browser-safe path.resolve (replaces Node `path` in Vite bundles). */
function normalizeSegments(segments) {
  const parts = []
  let absolute = false
  for (const segment of segments) {
    if (!segment) continue
    const normalized = String(segment).replace(/\\/g, '/')
    if (normalized.startsWith('/')) {
      parts.length = 0
      absolute = true
    }
    for (const part of normalized.split('/')) {
      if (!part || part === '.') continue
      if (part === '..') parts.pop()
      else parts.push(part)
    }
  }
  const joined = parts.join('/')
  if (absolute) return `/${joined}`
  return joined || '.'
}

function toAbsoluteRoutePath(result) {
  if (result === '.' || result === '') return '/'
  return result.startsWith('/') ? result : `/${result}`
}

export function resolve(...args) {
  return toAbsoluteRoutePath(normalizeSegments(args))
}

export default {
  resolve
}
