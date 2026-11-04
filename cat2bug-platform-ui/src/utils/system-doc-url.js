import { getVersion } from '@/api/version'

/** 官方文档根路径（无版本时回退） */
export const SYSTEM_DOC_BASE_URL = 'https://www.cat2bug.com/document/cat2bug-platform'

/** @deprecated 请用 buildSystemDocUrl / resolveSystemDocUrl；保留给未传版本的兜底 */
export const SYSTEM_DOC_URL = SYSTEM_DOC_BASE_URL

/**
 * 从应用版本号得到文档站路径段：1.1.0 → 1.1.*
 * @param {unknown} version
 * @returns {string|null}
 */
export function docVersionSegment(version) {
  if (version == null) {
    return null
  }
  const raw = String(version).trim()
  if (!raw) {
    return null
  }
  const normalized = raw.replace(/^v/i, '')
  const match = normalized.match(/^(\d+)\.(\d+)/)
  if (!match) {
    return null
  }
  return `${match[1]}.${match[2]}.*`
}

/**
 * @param {unknown} version 如 1.1.0
 * @returns {string}
 */
export function buildSystemDocUrl(version) {
  const segment = docVersionSegment(version)
  return segment ? `${SYSTEM_DOC_BASE_URL}/${segment}` : SYSTEM_DOC_BASE_URL
}

let cachedUrlPromise = null

/**
 * 按当前后端 `/version` 解析文档 URL（带缓存）。
 * @returns {Promise<string>}
 */
export function resolveSystemDocUrl() {
  if (!cachedUrlPromise) {
    cachedUrlPromise = getVersion()
      .then((res) => {
        const version = typeof res === 'string'
          ? res
          : (res && (res.data || res.msg || res.version)) || ''
        return buildSystemDocUrl(version)
      })
      .catch(() => SYSTEM_DOC_BASE_URL)
  }
  return cachedUrlPromise
}

/** 打开与当前版本对应的系统文档（新窗口） */
export function openSystemDoc() {
  return resolveSystemDocUrl().then((url) => {
    window.open(url)
    return url
  })
}
