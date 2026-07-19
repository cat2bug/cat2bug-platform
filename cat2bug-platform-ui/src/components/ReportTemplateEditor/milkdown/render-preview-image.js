import { getReportPreviewState, resolvePreviewPath } from './report-preview-data'
import { resolveProjectIconUrl, applyDefaultImageOnError } from '@/utils/upload-asset'
import {
  assignImgStyle,
  parseInlineStyle,
  parseRotateDeg,
  withPreviewImageFit
} from './preview-image-style'
import { applyImageLayoutDom, parseCssPx } from './image-variable-resize'

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

  const stage = document.createElement('div')
  stage.className = 'cat2bug-dsl-preview-image-stage'

  const img = document.createElement('img')
  img.className = 'cat2bug-dsl-preview-image'
  img.alt = attrs.path || 'preview'
  img.src = String(src)
  img.loading = 'lazy'
  applyDefaultImageOnError(img)

  const styleMap = withPreviewImageFit(parseInlineStyle(attrs.style))
  const rotate = parseRotateDeg(attrs.style || '')
  const width = parseCssPx(styleMap.width)
  const height = parseCssPx(styleMap.height)
  const imgStyle = { ...styleMap }
  delete imgStyle.transform
  assignImgStyle(img.style, imgStyle)

  stage.append(img)
  wrapper.append(stage)

  if (width && height) {
    applyImageLayoutDom(wrapper, stage, img, width, height, rotate)
  } else if (rotate) {
    stage.style.transform = `rotate(${rotate}deg)`
  }

  return wrapper
}
