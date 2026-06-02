import { isExternal } from '@/utils/validate'

/** 上传图片加载失败或路径无效时的统一占位图 */
export const DEFAULT_IMAGE = require('@/assets/images/default-image.svg')

/** el-image error 槽外层 class（与全局 cat2bug-image-error 样式配套） */
export const IMAGE_ERROR_SLOT_CLASS = 'cat2bug-image-error'

/** 原生 img 回退占位 class */
export const IMAGE_ERROR_IMG_CLASS = 'cat2bug-image-error__img'

/** 上传组件默认允许的常见 Web 图片扩展名（不含点） */
export const WEB_IMAGE_UPLOAD_EXTENSIONS = Object.freeze([
  'png',
  'jpg',
  'jpeg',
  'gif',
  'webp',
  'bmp',
  'svg',
  'ico',
  'avif'
])

const IMAGE_MIME_BY_EXT = Object.freeze({
  png: 'image/png',
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  gif: 'image/gif',
  webp: 'image/webp',
  bmp: 'image/bmp',
  svg: 'image/svg+xml',
  ico: 'image/x-icon',
  avif: 'image/avif'
})

/**
 * @param {string} [filename]
 * @returns {string}
 */
export function normalizeUploadFileExtension(filename) {
  if (!filename || filename.lastIndexOf('.') < 0) {
    return ''
  }
  return filename.slice(filename.lastIndexOf('.') + 1).toLowerCase()
}

/**
 * 生成 file input / el-upload 的 accept 字符串。
 * @param {readonly string[]} [extensions]
 * @returns {string}
 */
export function buildImageUploadAccept(extensions = WEB_IMAGE_UPLOAD_EXTENSIONS) {
  if (!extensions || !extensions.length) {
    return 'image/*'
  }
  const set = new Set()
  extensions.forEach((t) => {
    const ext = String(t).replace(/^\./, '').toLowerCase()
    if (!ext) return
    set.add(`.${ext}`)
    if (IMAGE_MIME_BY_EXT[ext]) {
      set.add(IMAGE_MIME_BY_EXT[ext])
    }
  })
  return set.size ? Array.from(set).join(',') : 'image/*'
}

/**
 * @param {File | { name?: string, type?: string } | null | undefined} file
 * @param {readonly string[]} [extensions]
 * @returns {boolean}
 */
export function isAllowedImageUploadFile(file, extensions = WEB_IMAGE_UPLOAD_EXTENSIONS) {
  if (!file) {
    return false
  }
  if (!extensions || !extensions.length) {
    return String(file.type || '').toLowerCase().startsWith('image/')
  }
  const ext = normalizeUploadFileExtension(file.name)
  const mime = String(file.type || '').toLowerCase()
  return extensions.some((type) => {
    const t = String(type).replace(/^\./, '').toLowerCase()
    if (!t) return false
    if (ext === t) return true
    const expectedMime = IMAGE_MIME_BY_EXT[t]
    if (expectedMime && mime === expectedMime) return true
    if (mime && mime.includes(t)) return true
    return false
  })
}

/** Vue 组件：el-image 的 error 槽，与 Cat2BugImage 共用 */
export { default as Cat2BugImageErrorSlot } from '@/components/Cat2BugImage/ImageErrorSlot.vue'

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
/**
 * 生成原生 img 的 onerror 内联脚本（Excel 单元格等无法挂 Vue 组件时使用）。
 * @param {string} [defaultSrc]
 * @returns {string}
 */
export function buildNativeImageOnerrorScript(defaultSrc = DEFAULT_IMAGE) {
  const src = String(defaultSrc).replace(/\\/g, '\\\\').replace(/'/g, "\\'")
  return `this.onerror=null;this.classList.add('${IMAGE_ERROR_IMG_CLASS}');this.src='${src}'`
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
    imgEl.classList.add(IMAGE_ERROR_IMG_CLASS)
    if (imgEl.src !== defaultSrc) {
      imgEl.onerror = null
      imgEl.src = defaultSrc
    }
  })
}
