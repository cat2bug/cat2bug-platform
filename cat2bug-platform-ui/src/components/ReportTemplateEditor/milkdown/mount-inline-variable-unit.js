import {
  createVariableUnitKey,
  getVariableDisplayMode,
  getVariableExpandGeneration,
  toggleVariableDisplayMode,
  clearVariableUnitOverride,
  subscribeVariableExpandState
} from './variable-expand-state'
import {
  ensureReportPreviewData,
  getActivePreviewProjectId,
  getReportPreviewState,
  subscribeReportPreviewData
} from './report-preview-data'
import { resolvePreviewPlanVariable, resolvePreviewVariable } from './render-preview-variable'
import { resolveVariableTitle } from './variable-title-labels'
import {
  createVariableHoverFloatController,
  createVariableUnitDomShell
} from './variable-hover-float'

const HOVER_LEAVE_DELAY_MS = 120

/**
 * Mount an interactive inline variable unit (same hover chrome / 文 ↔ ${} as Milkdown NodeViews).
 * Used for `${}` tokens embedded in plan-statistics HTML table cells.
 *
 * @param {{
 *   path: string,
 *   raw: string,
 *   planIndex?: number,
 *   isPlan?: boolean
 * }} attrs
 * @returns {{ dom: HTMLElement, destroy: () => void }}
 */
export function mountInlineVariableUnit(attrs) {
  const isPlan = !!attrs.isPlan || attrs.planIndex != null
  const unitKey = createVariableUnitKey()
  let generationSeen = getVariableExpandGeneration()
  let currentAttrs = {
    path: attrs.path,
    raw: attrs.raw,
    planIndex: attrs.planIndex
  }

  const { dom, frame, chrome, toggle, title, body } = createVariableUnitDomShell({ isPlan })
  const hoverFloat = createVariableHoverFloatController(dom, { frame, chrome, body })

  let paintToken = 0
  let hoverTimer = null

  function resolveDisplay() {
    return isPlan
      ? resolvePreviewPlanVariable({ path: currentAttrs.path, planIndex: currentAttrs.planIndex })
      : resolvePreviewVariable({ path: currentAttrs.path })
  }

  function applyBody(mode) {
    dom.classList.remove('is-content', 'is-variable', 'is-loading', 'is-empty')

    if (mode === 'variable') {
      body.textContent = currentAttrs.raw || ''
      dom.classList.add('is-variable')
      return
    }

    const result = resolveDisplay()
    dom.classList.add('is-content')
    if (result.kind === 'loading') {
      body.textContent = '…'
      dom.classList.add('is-loading')
      return
    }
    if (result.kind === 'error' || result.kind === 'empty') {
      body.textContent = '—'
      dom.classList.add('is-empty')
      return
    }
    body.textContent = result.text || ''
  }

  function applyToggle(mode) {
    toggle.textContent = mode === 'content' ? '文' : '${}'
    const label = mode === 'content' ? '切换为变量态' : '切换为内容态'
    toggle.setAttribute('aria-label', label)
    toggle.title = label
  }

  function applyTitle() {
    title.textContent = resolveVariableTitle(currentAttrs.path, {
      planIndex: currentAttrs.planIndex
    })
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
    destroy() {
      if (hoverTimer) {
        clearTimeout(hoverTimer)
        hoverTimer = null
      }
      hoverFloat.destroy()
      dom.removeEventListener('mouseenter', handleMouseEnter)
      dom.removeEventListener('mouseleave', handleMouseLeave)
      toggle.removeEventListener('mousedown', handleTogglePointerDown)
      toggle.removeEventListener('click', handleToggleClick)
      unsubscribePreview()
      unsubscribeExpand()
      clearVariableUnitOverride(unitKey)
    }
  }
}
