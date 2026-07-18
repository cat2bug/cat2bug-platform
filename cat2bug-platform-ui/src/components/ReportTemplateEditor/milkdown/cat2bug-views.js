import { $viewAsync } from '@milkdown/kit/utils'
import {
  cat2bugDefectTableNode,
  cat2bugCaseCardNode,
  cat2bugDynamicImageNode,
  cat2bugVariableNode,
  cat2bugPlanVariableNode,
  cat2bugRawBlockNode
} from './cat2bug-nodes'
import { subscribeReportPreviewData, ensureReportPreviewData, getReportPreviewState, getActivePreviewProjectId } from './report-preview-data'
import { renderPreviewTable } from './render-preview-table'
import { renderPreviewImage } from './render-preview-image'
import { renderPreviewCaseCard } from './render-preview-case-card'
import { renderPreviewPlanStatistics, disposePlanStatisticsPreview } from './render-preview-plan-statistics'
import { resolvePreviewVariable, resolvePreviewPlanVariable } from './render-preview-variable'
import {
  createVariableUnitKey,
  getVariableDisplayMode,
  toggleVariableDisplayMode,
  clearVariableUnitOverride,
  getVariableExpandGeneration,
  subscribeVariableExpandState
} from './variable-expand-state'
import { resolveVariableTitle } from './variable-title-labels'
import {
  createVariableHoverFloatController,
  createVariableUnitDomShell
} from './variable-hover-float'

const HOVER_LEAVE_DELAY_MS = 120

const BLOCK_META = {
  cat2bug_defect_table: { label: '缺陷表格', icon: '▦' },
  cat2bug_case_card: { label: '用例卡片', icon: '▤' },
  cat2bug_dynamic_image: { label: '动态图片', icon: '▣' },
  cat2bug_raw_block: { label: '保留块', icon: '▧' }
}

const RAW_BLOCK_HINTS = {
  'plan-statistics': '计划统计表：表格按 Markdown 展示，单元格内变量随工具栏展开/折起切换内容与 token。结构请在「源码」模式编辑。',
  'multimd-table': '合并单元格表格为只读保留块，请切换至「源码」模式编辑。',
  generic: '复杂 DSL / 表格片段为只读保留块，请切换至「源码」模式编辑。'
}

/** @returns {import('@milkdown/prose/view').NodeViewConstructor} */
function createRawBlockView() {
  return (node) => {
    const kind = node.attrs.kind || 'generic'
    if (kind === 'plan-statistics') {
      return createPlanStatisticsTableView(node)
    }

    const dom = document.createElement('div')
    dom.className = 'cat2bug-dsl-block__view cat2bug-dsl-block__view--raw'
    dom.contentEditable = 'false'

    const header = document.createElement('div')
    header.className = 'cat2bug-dsl-block__view-header'

    const icon = document.createElement('span')
    icon.className = 'cat2bug-dsl-block__view-icon'
    icon.textContent = BLOCK_META.cat2bug_raw_block.icon

    const title = document.createElement('span')
    title.className = 'cat2bug-dsl-block__view-title'
    title.textContent = BLOCK_META.cat2bug_raw_block.label

    header.append(icon, title)

    const hint = document.createElement('div')
    hint.className = 'cat2bug-dsl-block__view-hint'

    const pre = document.createElement('pre')
    pre.className = 'cat2bug-dsl-block__view-token cat2bug-dsl-block__view-token--raw'

    function paint(updated) {
      const nextKind = updated.attrs.kind || 'generic'
      hint.textContent = RAW_BLOCK_HINTS[nextKind] || RAW_BLOCK_HINTS.generic
      pre.textContent = updated.attrs.value || ''
    }

    paint(node)
    dom.append(header, hint, pre)

    return {
      dom,
      update(updated) {
        if (updated.type !== node.type) {
          return false
        }
        if ((updated.attrs.kind || 'generic') === 'plan-statistics') {
          return false
        }
        paint(updated)
        return true
      }
    }
  }
}

/** Plan-statistics: raw storage for round-trip; visual = HTML table + in-cell variables. */
function createPlanStatisticsTableView(node) {
  const dom = document.createElement('div')
  // Not a variable unit itself — only hosts the table; avoid block var hover chrome on the whole table.
  dom.className = 'cat2bug-dsl-preview-plan-stats-host'
  dom.contentEditable = 'false'

  const body = document.createElement('div')
  body.className = 'cat2bug-dsl-preview-plan-stats-host__body'
  dom.append(body)

  let currentAttrs = { ...node.attrs }
  let paintToken = 0
  /** @type {HTMLElement|null} */
  let previewRoot = null

  function paint() {
    const token = ++paintToken
    const render = () => {
      if (token !== paintToken) {
        return
      }
      try {
        disposePlanStatisticsPreview(previewRoot)
        previewRoot = renderPreviewPlanStatistics(currentAttrs.value || '')
        body.replaceChildren(previewRoot)
      } catch (error) {
        disposePlanStatisticsPreview(previewRoot)
        previewRoot = null
        const hint = document.createElement('div')
        hint.className = 'cat2bug-dsl-preview-plan-stats__hint cat2bug-dsl-preview-plan-stats__hint--error'
        hint.textContent = '计划统计表预览渲染失败'
        body.replaceChildren(hint)
        if (import.meta.env.DEV) {
          console.error('[ReportTemplateEditor] plan statistics preview failed:', error)
        }
      }
    }
    render()
    const snapshot = getReportPreviewState()
    const projectId = getActivePreviewProjectId()
    if (projectId && (snapshot.loading || !snapshot.data)) {
      ensureReportPreviewData(projectId).finally(render)
    }
  }

  paint()
  // In-cell variable units subscribe to expand state themselves; only re-render table on data refresh.
  const unsubscribePreview = subscribeReportPreviewData(paint)

  return {
    dom,
    update(updated) {
      if (updated.type !== node.type) {
        return false
      }
      if ((updated.attrs.kind || 'generic') !== 'plan-statistics') {
        return false
      }
      currentAttrs = { ...updated.attrs }
      paint()
      return true
    },
    destroy() {
      disposePlanStatisticsPreview(previewRoot)
      previewRoot = null
      unsubscribePreview()
    }
  }
}

/**
 * Variable unit NodeView: production-aligned inline plain text by default;
 * hover reveals dashed-border chrome with a two-state toggle (content/variable) and a title row.
 * @param {{ isPlan?: boolean, resolveDisplay: (attrs: object) => { kind: string, text?: string }, resolveTitle: (attrs: object) => string }} options
 * @returns {import('@milkdown/prose/view').NodeViewConstructor}
 */
function createVariableUnitView({ isPlan = false, resolveDisplay, resolveTitle }) {
  return (node) => {
    const unitKey = createVariableUnitKey()
    let generationSeen = getVariableExpandGeneration()

    const { dom, frame, chrome, toggle, title, body } = createVariableUnitDomShell({ isPlan })
    const hoverFloat = createVariableHoverFloatController(dom, { frame, chrome, body })

    let currentAttrs = { ...node.attrs }
    let paintToken = 0
    let hoverTimer = null

    function applyBody(mode) {
      dom.classList.remove('is-content', 'is-variable', 'is-loading', 'is-empty')

      if (mode === 'variable') {
        body.textContent = currentAttrs.raw || ''
        dom.classList.add('is-variable')
        return
      }

      const result = resolveDisplay(currentAttrs)
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
      title.textContent = resolveTitle(currentAttrs)
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

/**
 * Block-level DSL unit NodeView ($table / $card / $image): same hover/two-state
 * contract as {@link createVariableUnitView}, adapted for block content (tables,
 * images, card lists) instead of inline text. Default (not hovered) renders only
 * the current-mode content with no border/toggle/title, matching the production
 * preview; hover reveals the dashed-border chrome with the two-state toggle and title.
 * @param {{ resolveTitle: (attrs: object) => string, renderContent: (attrs: object) => Node, viewModifierClass?: string }} options
 * @returns {import('@milkdown/prose/view').NodeViewConstructor}
 */
function createDslTokenBlockView({ resolveTitle, renderContent, viewModifierClass = '' }) {
  return (node) => {
    const unitKey = createVariableUnitKey()
    let generationSeen = getVariableExpandGeneration()

    const { dom, frame, chrome, toggle, title, body } = createVariableUnitDomShell({ isBlock: true })
    if (viewModifierClass) {
      dom.classList.add(viewModifierClass)
    }
    const hoverFloat = createVariableHoverFloatController(dom, { frame, chrome, body })

    let currentAttrs = { ...node.attrs }
    let paintToken = 0
    let hoverTimer = null

    function applyBody(mode) {
      dom.classList.remove('is-content', 'is-variable')

      if (mode === 'variable') {
        body.textContent = currentAttrs.raw || ''
        dom.classList.add('is-variable')
        return
      }

      dom.classList.add('is-content')
      try {
        body.replaceChildren(renderContent(currentAttrs))
      } catch (error) {
        const hint = document.createElement('div')
        hint.className = 'cat2bug-dsl-preview-table__hint cat2bug-dsl-preview-table__hint--error'
        hint.textContent = '预览渲染失败'
        body.replaceChildren(hint)
        if (import.meta.env.DEV) {
          console.error('[ReportTemplateEditor] block preview failed:', error)
        }
      }
    }

    function applyToggle(mode) {
      toggle.textContent = mode === 'content' ? '文' : '${}'
      const label = mode === 'content' ? '切换为变量态' : '切换为内容态'
      toggle.setAttribute('aria-label', label)
      toggle.title = label
    }

    function applyTitle() {
      title.textContent = resolveTitle(currentAttrs)
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

export const cat2bugDefectTableView = $viewAsync(
  cat2bugDefectTableNode,
  () => createDslTokenBlockView({
    resolveTitle: () => BLOCK_META.cat2bug_defect_table.label,
    renderContent: (attrs) => renderPreviewTable(attrs),
    viewModifierClass: 'cat2bug-dsl-var__view--table'
  }),
  'Cat2bugDefectTableView'
)

export const cat2bugCaseCardView = $viewAsync(
  cat2bugCaseCardNode,
  () => createDslTokenBlockView({
    resolveTitle: () => BLOCK_META.cat2bug_case_card.label,
    renderContent: (attrs) => renderPreviewCaseCard(attrs),
    viewModifierClass: 'cat2bug-dsl-var__view--card'
  }),
  'Cat2bugCaseCardView'
)

export const cat2bugDynamicImageView = $viewAsync(
  cat2bugDynamicImageNode,
  () => createDslTokenBlockView({
    resolveTitle: () => BLOCK_META.cat2bug_dynamic_image.label,
    renderContent: (attrs) => renderPreviewImage(attrs),
    viewModifierClass: 'cat2bug-dsl-var__view--image'
  }),
  'Cat2bugDynamicImageView'
)

export const cat2bugVariableView = $viewAsync(
  cat2bugVariableNode,
  () => createVariableUnitView({
    resolveDisplay: resolvePreviewVariable,
    resolveTitle: (attrs) => resolveVariableTitle(attrs.path)
  }),
  'Cat2bugVariableView'
)

export const cat2bugPlanVariableView = $viewAsync(
  cat2bugPlanVariableNode,
  () => createVariableUnitView({
    isPlan: true,
    resolveDisplay: resolvePreviewPlanVariable,
    resolveTitle: (attrs) => resolveVariableTitle(attrs.path, { planIndex: attrs.planIndex })
  }),
  'Cat2bugPlanVariableView'
)

export const cat2bugRawBlockView = $viewAsync(
  cat2bugRawBlockNode,
  () => createRawBlockView(),
  'Cat2bugRawBlockView'
)
