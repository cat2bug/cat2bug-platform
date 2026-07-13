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
import { resolvePreviewVariable, resolvePreviewPlanVariable } from './render-preview-variable'

const BLOCK_META = {
  cat2bug_defect_table: { label: '缺陷表格', icon: '▦' },
  cat2bug_case_card: { label: '用例卡片', icon: '▤' },
  cat2bug_dynamic_image: { label: '动态图片', icon: '▣' },
  cat2bug_raw_block: { label: '保留块', icon: '▧' }
}

const RAW_BLOCK_HINTS = {
  'plan-statistics': '计划统计大表为只读保留块，请在「源码」模式中编辑表格结构与计划变量。',
  'multimd-table': '合并单元格表格为只读保留块，请切换至「源码」模式编辑。',
  generic: '复杂 DSL / 表格片段为只读保留块，请切换至「源码」模式编辑。'
}

/** @returns {import('@milkdown/prose/view').NodeViewConstructor} */
function createBlockView(meta) {
  return (node) => {
    const dom = document.createElement('div')
    dom.className = 'cat2bug-dsl-block__view'
    dom.contentEditable = 'false'

    const header = document.createElement('div')
    header.className = 'cat2bug-dsl-block__view-header'

    const icon = document.createElement('span')
    icon.className = 'cat2bug-dsl-block__view-icon'
    icon.textContent = meta.icon

    const title = document.createElement('span')
    title.className = 'cat2bug-dsl-block__view-title'
    title.textContent = meta.label

    header.append(icon, title)

    const code = document.createElement('code')
    code.className = 'cat2bug-dsl-block__view-token'
    code.textContent = node.attrs.raw || ''

    dom.append(header, code)

    return {
      dom,
      update(updated) {
        if (updated.type !== node.type) {
          return false
        }
        code.textContent = updated.attrs.raw || ''
        return true
      }
    }
  }
}

/** @returns {import('@milkdown/prose/view').NodeViewConstructor} */
function createDefectTableView() {
  return (node) => {
    const dom = document.createElement('div')
    dom.className = 'cat2bug-dsl-block__view cat2bug-dsl-block__view--live-table'
    dom.contentEditable = 'false'

    const header = document.createElement('div')
    header.className = 'cat2bug-dsl-block__view-header'

    const icon = document.createElement('span')
    icon.className = 'cat2bug-dsl-block__view-icon'
    icon.textContent = BLOCK_META.cat2bug_defect_table.icon

    const title = document.createElement('span')
    title.className = 'cat2bug-dsl-block__view-title'
    title.textContent = BLOCK_META.cat2bug_defect_table.label

    header.append(icon, title)

    const body = document.createElement('div')
    body.className = 'cat2bug-dsl-block__view-body'

    dom.append(header, body)

    let currentAttrs = { ...node.attrs }
    let paintToken = 0

    function paint() {
      const token = ++paintToken
      const render = () => {
        if (token !== paintToken) {
          return
        }
        try {
          body.replaceChildren(renderPreviewTable(currentAttrs))
        } catch (error) {
          const hint = document.createElement('div')
          hint.className = 'cat2bug-dsl-preview-table__hint cat2bug-dsl-preview-table__hint--error'
          hint.textContent = '表格预览渲染失败'
          body.replaceChildren(hint)
          if (import.meta.env.DEV) {
            console.error('[ReportTemplateEditor] defect table preview failed:', error)
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
    const unsubscribe = subscribeReportPreviewData(paint)

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
        unsubscribe()
      }
    }
  }
}

/** @returns {import('@milkdown/prose/view').NodeViewConstructor} */
function createDynamicImageView() {
  return (node) => {
    const dom = document.createElement('div')
    dom.className = 'cat2bug-dsl-block__view cat2bug-dsl-block__view--live-image'
    dom.contentEditable = 'false'

    const header = document.createElement('div')
    header.className = 'cat2bug-dsl-block__view-header'

    const icon = document.createElement('span')
    icon.className = 'cat2bug-dsl-block__view-icon'
    icon.textContent = BLOCK_META.cat2bug_dynamic_image.icon

    const title = document.createElement('span')
    title.className = 'cat2bug-dsl-block__view-title'
    title.textContent = BLOCK_META.cat2bug_dynamic_image.label

    header.append(icon, title)

    const body = document.createElement('div')
    body.className = 'cat2bug-dsl-block__view-body'

    dom.append(header, body)

    let currentAttrs = { ...node.attrs }
    let paintToken = 0

    function paint() {
      const token = ++paintToken
      const render = () => {
        if (token !== paintToken) {
          return
        }
        body.replaceChildren(renderPreviewImage(currentAttrs))
      }
      render()
      const snapshot = getReportPreviewState()
      const projectId = getActivePreviewProjectId()
      if (projectId && (snapshot.loading || !snapshot.data)) {
        ensureReportPreviewData(projectId).finally(render)
      }
    }

    paint()
    const unsubscribe = subscribeReportPreviewData(paint)

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
        unsubscribe()
      }
    }
  }
}

/** @returns {import('@milkdown/prose/view').NodeViewConstructor} */
function createRawBlockView() {
  return (node) => {
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
      const kind = updated.attrs.kind || 'generic'
      hint.textContent = RAW_BLOCK_HINTS[kind] || RAW_BLOCK_HINTS.generic
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
        paint(updated)
        return true
      }
    }
  }
}

/** @returns {import('@milkdown/prose/view').NodeViewConstructor} */
function createLiveChipView(className, resolveDisplay) {
  return (node) => {
    const dom = document.createElement('span')
    dom.className = `cat2bug-dsl-chip__view ${className}`
    dom.contentEditable = 'false'

    let currentAttrs = { ...node.attrs }
    let paintToken = 0

    function applyDisplay(result) {
      dom.classList.remove('is-loading', 'is-resolved', 'is-empty')
      dom.removeAttribute('title')

      if (result.kind === 'loading') {
        dom.textContent = '…'
        dom.classList.add('is-loading')
        return
      }
      if (result.kind === 'error' || result.kind === 'empty') {
        dom.textContent = '—'
        dom.classList.add('is-empty')
        if (currentAttrs.raw) {
          dom.title = currentAttrs.raw
        }
        return
      }

      dom.textContent = result.text || ''
      dom.classList.add('is-resolved')
      if (currentAttrs.raw) {
        dom.title = currentAttrs.raw
      }
    }

    function paint() {
      const token = ++paintToken
      const render = () => {
        if (token !== paintToken) {
          return
        }
        applyDisplay(resolveDisplay(currentAttrs))
      }
      render()
      const snapshot = getReportPreviewState()
      const projectId = getActivePreviewProjectId()
      if (projectId && (snapshot.loading || !snapshot.data)) {
        ensureReportPreviewData(projectId).finally(render)
      }
    }

    paint()
    const unsubscribe = subscribeReportPreviewData(paint)

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
        unsubscribe()
      }
    }
  }
}

export const cat2bugDefectTableView = $viewAsync(
  cat2bugDefectTableNode,
  () => createDefectTableView(),
  'Cat2bugDefectTableView'
)

export const cat2bugCaseCardView = $viewAsync(
  cat2bugCaseCardNode,
  () => createBlockView(BLOCK_META.cat2bug_case_card),
  'Cat2bugCaseCardView'
)

export const cat2bugDynamicImageView = $viewAsync(
  cat2bugDynamicImageNode,
  () => createDynamicImageView(),
  'Cat2bugDynamicImageView'
)

export const cat2bugVariableView = $viewAsync(
  cat2bugVariableNode,
  () => createLiveChipView('cat2bug-dsl-chip__view--variable', resolvePreviewVariable),
  'Cat2bugVariableView'
)

export const cat2bugPlanVariableView = $viewAsync(
  cat2bugPlanVariableNode,
  () => createLiveChipView('cat2bug-dsl-chip__view--plan', resolvePreviewPlanVariable),
  'Cat2bugPlanVariableView'
)

export const cat2bugRawBlockView = $viewAsync(
  cat2bugRawBlockNode,
  () => createRawBlockView(),
  'Cat2bugRawBlockView'
)
