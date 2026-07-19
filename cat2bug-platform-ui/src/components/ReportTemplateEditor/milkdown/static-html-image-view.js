import { $view } from '@milkdown/kit/utils'
import { htmlSchema } from '@milkdown/kit/preset/commonmark'
import { mountImageResizeHandle, parseCssPx, applyImageLayoutDom } from './image-variable-resize'
import {
  assignImgStyle,
  parseInlineStyle,
  parseRotateDeg,
  withPreviewImageFit
} from './preview-image-style'
import { applyDefaultImageOnError } from '@/utils/upload-asset'
import { parseImgHtml, serializeImgHtml } from './static-html-image'
import { resolveStaticReportImageSrc } from './rewrite-report-snapshot-images'

export { parseImgHtml, serializeImgHtml } from './static-html-image'
export { resolveStaticReportImageSrc, rewriteReportSnapshotImageSrcs } from './rewrite-report-snapshot-images'

/**
 * Render Milkdown `html` atoms that are `<img>` as real images with resize handles.
 * Non-img HTML keeps the default text atom rendering.
 * @returns {import('@milkdown/prose/view').NodeViewConstructor}
 */
export function createStaticHtmlImageNodeView() {
  return (node, view, getPos) => {
    let currentValue = node.attrs.value || ''
    let meta = parseImgHtml(currentValue)

    if (!meta) {
      const span = document.createElement('span')
      span.setAttribute('data-type', 'html')
      span.setAttribute('data-value', currentValue)
      span.textContent = currentValue
      return {
        dom: span,
        update(updated) {
          if (updated.type.name !== 'html') {
            return false
          }
          const next = updated.attrs.value || ''
          if (parseImgHtml(next)) {
            return false
          }
          currentValue = next
          span.textContent = next
          span.setAttribute('data-value', next)
          return true
        }
      }
    }

    const wrap = document.createElement('span')
    wrap.className = 'cat2bug-dsl-preview-image-wrap cat2bug-static-html-image'
    wrap.setAttribute('data-type', 'html-img')
    wrap.contentEditable = 'false'

    const stage = document.createElement('div')
    stage.className = 'cat2bug-dsl-preview-image-stage'

    const img = document.createElement('img')
    img.className = 'cat2bug-dsl-preview-image'
    img.loading = 'lazy'
    applyDefaultImageOnError(img)
    stage.appendChild(img)
    wrap.appendChild(stage)

    /** @type {{ destroy: () => void } | null} */
    let resizeApi = null
    let selected = false

    function paintFromMeta() {
      img.src = resolveStaticReportImageSrc(meta.src || '')
      img.alt = meta.alt || ''
      const styleMap = withPreviewImageFit(parseInlineStyle(meta.style || ''))
      const rotate = parseRotateDeg(meta.style || '')
      const width = parseCssPx(styleMap.width)
      const height = parseCssPx(styleMap.height)
      const imgStyle = { ...styleMap }
      delete imgStyle.transform
      assignImgStyle(img.style, imgStyle)
      if (width && height) {
        applyImageLayoutDom(wrap, stage, img, width, height, rotate)
      } else if (rotate) {
        stage.style.transform = `rotate(${rotate}deg)`
      }
    }

    function clearResizeHandle() {
      resizeApi?.destroy()
      resizeApi = null
      wrap.classList.remove('is-resizable')
    }

    function commitStyle(style) {
      const pos = typeof getPos === 'function' ? getPos() : null
      if (typeof pos !== 'number') {
        return
      }
      meta = { ...meta, style: style || '' }
      currentValue = serializeImgHtml(meta)
      const tr = view.state.tr.setNodeMarkup(pos, undefined, {
        value: currentValue
      })
      view.dispatch(tr)
    }

    function ensureResizeHandle() {
      if (resizeApi) {
        return
      }
      wrap.classList.add('is-resizable')
      resizeApi = mountImageResizeHandle(wrap, {
        getAttrs: () => ({ style: meta.style || '', path: '' }),
        getView: () => view,
        getPos,
        onCommit: ({ style }) => commitStyle(style),
        onReset: ({ style }) => commitStyle(style)
      })
    }

    function syncSelectionChrome() {
      wrap.classList.toggle('is-selected', selected)
      if (selected) {
        ensureResizeHandle()
      } else {
        clearResizeHandle()
      }
    }

    paintFromMeta()

    return {
      dom: wrap,
      selectNode() {
        selected = true
        syncSelectionChrome()
      },
      deselectNode() {
        selected = false
        syncSelectionChrome()
      },
      stopEvent(event) {
        const target = event.target
        return target instanceof Element
          && !!target.closest(
            '.cat2bug-dsl-preview-image__controls, .cat2bug-dsl-preview-image__resize, .cat2bug-dsl-preview-image__reset'
          )
      },
      ignoreMutation() {
        return true
      },
      update(updated) {
        if (updated.type.name !== 'html') {
          return false
        }
        const next = updated.attrs.value || ''
        const nextMeta = parseImgHtml(next)
        if (!nextMeta) {
          return false
        }
        currentValue = next
        meta = nextMeta
        paintFromMeta()
        if (selected) {
          clearResizeHandle()
          ensureResizeHandle()
        }
        return true
      },
      destroy() {
        clearResizeHandle()
      }
    }
  }
}

export const staticHtmlImageView = $view(htmlSchema.node, () => createStaticHtmlImageNodeView())
