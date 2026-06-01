import { isExternal } from '@/utils/validate'

/** 上传图片加载失败或路径无效时的统一占位图 */
export const DEFAULT_IMAGE = require('@/assets/images/default-image.svg')

/** @param {string | null | undefined} value */
export function isNonEmptyUploadPath(value) {
  if (value == null || typeof value !== 'string') return false
  const trimmed = value.trim()
  return !!trimmed && trimmed !== 'null' && trimmed !== 'undefined'
}

/**
 * 解析单条上传资源路径为可访问 URL。
 * @param {string | null | undefined} path
 * @param {string} [baseApi]
 * @returns {string}
 */
export function resolveUploadUrl(path, baseApi = process.env.VUE_APP_BASE_API || '') {
  if (!isNonEmptyUploadPath(path)) {
    return DEFAULT_IMAGE
  }
  const trimmed = path.trim()
  if (isExternal(trimmed) || trimmed.startsWith('data:')) {
    return trimmed
  }
  if (trimmed.startsWith('/static/') || trimmed.indexOf('/static/') > -1) {
    return trimmed
  }
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
    return trimmed
  }
  if (trimmed.startsWith(baseApi)) {
    return trimmed
  }
  if (trimmed.startsWith('/')) {
    return baseApi + trimmed
  }
  return baseApi + trimmed
}

/**
 * 解析逗号分隔的上传路径列表。
 * @param {string | string[] | null | undefined} imgUrls
 * @param {string} [baseApi]
 * @returns {string[]}
 */
export function resolveCommaSeparatedUrls(imgUrls, baseApi = process.env.VUE_APP_BASE_API || '') {
  if (!imgUrls) {
    return []
  }
  const parts = Array.isArray(imgUrls) ? imgUrls : String(imgUrls).split(',')
  return parts
    .map((item) => (typeof item === 'string' ? item.trim() : item))
    .filter(Boolean)
    .map((item) => resolveUploadUrl(item, baseApi))
}

/**
 * @param {string | null | undefined} teamIcon
 * @param {string} [baseApi]
 */
export function resolveTeamIconUrl(teamIcon, baseApi = process.env.VUE_APP_BASE_API || '') {
  return resolveUploadUrl(teamIcon, baseApi)
}

/**
 * @param {string | null | undefined} projectIcon
 * @param {string} [baseApi]
 */
export function resolveProjectIconUrl(projectIcon, baseApi = process.env.VUE_APP_BASE_API || '') {
  if (!isNonEmptyUploadPath(projectIcon)) {
    return DEFAULT_IMAGE
  }
  const trimmed = projectIcon.trim()
  if (isExternal(trimmed) || trimmed.startsWith('data:')) {
    return trimmed
  }
  if (trimmed.startsWith('/static/') || trimmed.indexOf('/static/') > -1) {
    return trimmed
  }
  if (trimmed.startsWith(baseApi)) {
    return trimmed
  }
  return resolveUploadUrl(trimmed, baseApi)
}

/**
 * 为原生 img 绑定加载失败时回退占位图。
 * @param {HTMLImageElement | null | undefined} imgEl
 * @param {string} [defaultSrc]
 */
export function applyDefaultImageOnError(imgEl, defaultSrc = DEFAULT_IMAGE) {
  if (!imgEl || imgEl.__cat2bugDefaultImgBound) {
    return
  }
  imgEl.__cat2bugDefaultImgBound = true
  imgEl.addEventListener('error', () => {
    if (imgEl.src !== defaultSrc) {
      imgEl.onerror = null
      imgEl.src = defaultSrc
    }
  })
}
