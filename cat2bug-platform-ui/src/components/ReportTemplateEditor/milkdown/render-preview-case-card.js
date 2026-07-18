import { getPreviewRows, getReportPreviewState } from './report-preview-data'

function formatCaseId(row) {
  const num = row?.caseNum ?? row?.id
  if (num == null || num === '') {
    return ''
  }
  return `#${num}`
}

/**
 * Compact case-card preview (id + name) built from the same preview dataset
 * as Cat2BugMarkdown's CaseCardPlugin (api.case.list). Intentionally lighter
 * than the production plugin's full step-table markup — good enough to
 * recognize which cases will render, without duplicating that logic here.
 * @param {{ dataSource?: string }} attrs
 */
export function renderPreviewCaseCard(attrs) {
  const wrapper = document.createElement('div')
  wrapper.className = 'cat2bug-dsl-preview-card-wrap'

  const { loading, error } = getReportPreviewState()
  const rows = getPreviewRows(attrs.dataSource || 'api.case.list')

  if (loading) {
    const hint = document.createElement('div')
    hint.className = 'cat2bug-dsl-preview-card__hint'
    hint.textContent = '加载中…'
    wrapper.append(hint)
    return wrapper
  }

  if (error) {
    const hint = document.createElement('div')
    hint.className = 'cat2bug-dsl-preview-card__hint cat2bug-dsl-preview-card__hint--error'
    hint.textContent = '预览数据加载失败'
    wrapper.append(hint)
    return wrapper
  }

  if (!rows.length) {
    const hint = document.createElement('div')
    hint.className = 'cat2bug-dsl-preview-card__hint'
    hint.textContent = '暂无测试用例'
    wrapper.append(hint)
    return wrapper
  }

  const list = document.createElement('div')
  list.className = 'cat2bug-dsl-preview-card__list'

  rows.forEach((row) => {
    const item = document.createElement('div')
    item.className = 'cat2bug-dsl-preview-card__item'

    const id = document.createElement('span')
    id.className = 'cat2bug-dsl-preview-card__id'
    id.textContent = formatCaseId(row)

    const name = document.createElement('span')
    name.className = 'cat2bug-dsl-preview-card__name'
    name.textContent = row.caseName || ''
    name.title = row.caseName || ''

    item.append(id, name)
    list.append(item)
  })

  wrapper.append(list)

  const meta = document.createElement('div')
  meta.className = 'cat2bug-dsl-preview-card__meta'
  meta.textContent = `共 ${rows.length} 条`
  wrapper.append(meta)

  return wrapper
}
