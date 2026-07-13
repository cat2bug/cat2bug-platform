import { getReportPreviewState, resolvePreviewPath } from './report-preview-data'
import { resolveProjectIconUrl, applyDefaultImageOnError } from '@/utils/upload-asset'

function parseInlineStyle(styleText) {
  const style = {}
  if (!styleText) {
    return style
  }
  styleText.split(';').forEach((chunk) => {
    const part = chunk.trim()
    if (!part) {
      return
    }
    const colon = part.indexOf(':')
    if (colon === -1) {
      return
    }
    const key = part.slice(0, colon).trim()
    const value = part.slice(colon + 1).trim()
    if (key) {
      style[key] = value
    }
  })
  return style
}

/**
 * @param {{ style?: string, path?: string }} attrs
 */
export function renderPreviewImage(attrs) {
  const wrapper = document.createElement('div')
  wrapper.className = 'cat2bug-dsl-preview-image-wrap'

  const { loading, projectId, error } = getReportPreviewState()
  if (loading) {
    const hint = document.createElement('div')
    hint.className = 'cat2bug-dsl-preview-image__hint'
    hint.textContent = '加载中…'
    wrapper.append(hint)
    return wrapper
  }

  if (error) {
    const hint = document.createElement('div')
    hint.className = 'cat2bug-dsl-preview-image__hint cat2bug-dsl-preview-image__hint--error'
    hint.textContent = '预览数据加载失败'
    wrapper.append(hint)
    return wrapper
  }

  const rawSrc = resolvePreviewPath(attrs.path)
  const src = attrs.path === 'api.project.projectIcon'
    ? resolveProjectIconUrl(rawSrc)
    : rawSrc
  if (!src) {
    const hint = document.createElement('div')
    hint.className = 'cat2bug-dsl-preview-image__hint'
    hint.textContent = projectId ? '暂无图片' : '未选择项目'
    wrapper.append(hint)
    return wrapper
  }

  const img = document.createElement('img')
  img.className = 'cat2bug-dsl-preview-image'
  img.alt = attrs.path || 'preview'
  img.src = String(src)
  img.loading = 'lazy'
  applyDefaultImageOnError(img)

  const style = parseInlineStyle(attrs.style)
  Object.assign(img.style, style)

  wrapper.append(img)
  return wrapper
}
