import { renderPreviewImage } from './render-preview-image'
import { mountImageResizeHandle } from './image-variable-resize'
import {
  createVariableUnitKey,
  getVariableDisplayMode,
  toggleVariableDisplayMode,
  clearVariableUnitOverride,
  getVariableExpandGeneration,
  subscribeVariableExpandState
} from './variable-expand-state'
import {
  createVariableHoverFloatController,
  createVariableUnitDomShell
} from './variable-hover-float'
import {
  subscribeReportPreviewData,
  ensureReportPreviewData,
  getReportPreviewState,
  getActivePreviewProjectId
} from './report-preview-data'

const HOVER_LEAVE_DELAY_MS = 120

/**
 * NodeView for `$image` — content/variable toggle plus SE resize when selected.
 * @returns {import('@milkdown/prose/view').NodeViewConstructor}
 */
export function createDynamicImageNodeView() {
  return (node, view, getPos) => {
    const unitKey = createVariableUnitKey()
    let generationSeen = getVariableExpandGeneration()

    const { dom, frame, chrome, toggle, title, body } = createVariableUnitDomShell({ isBlock: false })
    dom.classList.add('cat2bug-dsl-var__view--image')
    const hoverFloat = createVariableHoverFloatController(dom, { frame, chrome, body })

    let currentAttrs = { ...node.attrs }
    let paintToken = 0
    let hoverTimer = null
    /** @type {{ handle: HTMLElement, destroy: () => void } | null} */
    let resizeApi = null
    let selected = false

    function clearResizeHandle() {
      resizeApi?.destroy()
      resizeApi = null
    }

    function ensureResizeHandle() {
      if (resizeApi || getVariableDisplayMode(unitKey) !== 'content') {
        return
      }
      const wrap = body.querySelector('.cat2bug-dsl-preview-image-wrap')
      if (!(wrap instanceof HTMLElement)) {
        return
      }
      wrap.classList.add('is-resizable')
      resizeApi = mountImageResizeHandle(wrap, {
        getAttrs: () => currentAttrs,
        getView: () => view,
        getPos
      })
    }

    function syncSelectionChrome() {
      dom.classList.toggle('is-selected', selected)
      if (selected) {
        ensureResizeHandle()
      } else {
        clearResizeHandle()
        body.querySelector('.cat2bug-dsl-preview-image-wrap')?.classList.remove('is-resizable')
      }
    }

    function applyBody(mode) {
      clearResizeHandle()
      dom.classList.remove('is-content', 'is-variable')

      if (mode === 'variable') {
        body.textContent = currentAttrs.raw || ''
        dom.classList.add('is-variable')
        return
      }

      dom.classList.add('is-content')
      try {
        body.replaceChildren(renderPreviewImage(currentAttrs))
      } catch (error) {
        const hint = document.createElement('div')
        hint.className = 'cat2bug-dsl-preview-image__hint cat2bug-dsl-preview-image__hint--error'
        hint.textContent = '预览渲染失败'
        body.replaceChildren(hint)
        if (import.meta.env.DEV) {
          console.error('[ReportTemplateEditor] image preview failed:', error)
        }
      }
      if (selected) {
        ensureResizeHandle()
      }
    }

    function applyToggle(mode) {
      toggle.textContent = mode === 'content' ? '文' : '${}'
      const label = mode === 'content' ? '切换为变量态' : '切换为内容态'
      toggle.setAttribute('aria-label', label)
      toggle.title = label
    }

    function applyTitle() {
      title.textContent = '动态图片'
    }

    function paint() {
      const token = ++paintToken
      const mode = getVariableDisplayMode(unitKey)
      const render = () => {
        if (token !== paintToken) {
          return
        }
        applyBody(mode)
        applyToggle(mode)
        applyTitle()
        syncSelectionChrome()
        hoverFloat.sync()
      }
      render()
      if (mode === 'content') {
        const snapshot = getReportPreviewState()
        const projectId = getActivePreviewProjectId()
        if (projectId && (snapshot.loading || !snapshot.data)) {
          ensureReportPreviewData(projectId).finally(render)
        }
      }
    }

    function handleMouseEnter() {
      if (hoverTimer) {
        clearTimeout(hoverTimer)
        hoverTimer = null
      }
      hoverFloat.activate()
    }

    function handleMouseLeave() {
      if (hoverTimer) {
        clearTimeout(hoverTimer)
      }
      hoverTimer = setTimeout(() => {
        hoverTimer = null
        hoverFloat.deactivate()
      }, HOVER_LEAVE_DELAY_MS)
    }

    function handleTogglePointerDown(event) {
      event.preventDefault()
    }

    function handleToggleClick(event) {
      event.preventDefault()
      event.stopPropagation()
      toggleVariableDisplayMode(unitKey)
      paint()
    }

    function handleExpandStateChange(snapshot) {
      if (snapshot.generation !== generationSeen) {
        generationSeen = snapshot.generation
        clearVariableUnitOverride(unitKey)
      }
      paint()
    }

    dom.addEventListener('mouseenter', handleMouseEnter)
    dom.addEventListener('mouseleave', handleMouseLeave)
    toggle.addEventListener('mousedown', handleTogglePointerDown)
    toggle.addEventListener('click', handleToggleClick)

    paint()
    const unsubscribePreview = subscribeReportPreviewData(paint)
    const unsubscribeExpand = subscribeVariableExpandState(handleExpandStateChange)

    return {
      dom,
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
        if (updated.type !== node.type) {
          return false
        }
        currentAttrs = { ...updated.attrs }
        paint()
        return true
      },
      destroy() {
        if (hoverTimer) {
          clearTimeout(hoverTimer)
          hoverTimer = null
        }
        clearResizeHandle()
        hoverFloat.destroy()
        dom.removeEventListener('mouseenter', handleMouseEnter)
        dom.removeEventListener('mouseleave', handleMouseLeave)
        toggle.removeEventListener('mousedown', handleTogglePointerDown)
        toggle.removeEventListener('click', handleToggleClick)
        unsubscribePreview()
        unsubscribeExpand()
      }
    }
  }
}
