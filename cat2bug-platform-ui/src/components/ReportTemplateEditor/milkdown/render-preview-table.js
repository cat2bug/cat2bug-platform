import i18n from '@/utils/i18n/i18n'
import { DEFECT_TABLE_DEFAULT_COLUMNS } from '../dsl/grammar'
import { getPreviewRows, getReportPreviewState } from './report-preview-data'

export function columnsFromJson(json) {
  try {
    return JSON.parse(json || '[]')
  } catch (_e) {
    return []
  }
}

function resolveColumns(columnsJson, dataSource) {
  const parsed = columnsFromJson(columnsJson)
  if (parsed.length) {
    return parsed
  }
  if (dataSource === 'api.defect.list') {
    return DEFECT_TABLE_DEFAULT_COLUMNS.map((field) => ({ field, label: null }))
  }
  return []
}

function columnHeader(col) {
  if (col.label) {
    return i18n.te(col.label) ? i18n.t(col.label) : col.label
  }
  if (i18n.te(col.field)) {
    return i18n.t(col.field)
  }
  return col.field
}

function formatCell(value) {
  if (value == null || value === '') {
    return ''
  }
  if (Array.isArray(value)) {
    return value.length ? `${value.length}` : ''
  }
  if (typeof value === 'object') {
    return ''
  }
  const text = String(value)
  return text.length > 120 ? `${text.slice(0, 120)}…` : text
}

/**
 * Build a live preview table DOM (same data source as Cat2BugMarkdown TablePlugin).
 * @param {{ dataSource: string, columnsJson: string }} attrs
 */
export function renderPreviewTable(attrs) {
  const wrapper = document.createElement('div')
  wrapper.className = 'cat2bug-dsl-preview-table-wrap'

  const { loading, error } = getReportPreviewState()
  const columns = resolveColumns(attrs.columnsJson, attrs.dataSource)
  const rows = getPreviewRows(attrs.dataSource)

  if (loading) {
    const hint = document.createElement('div')
    hint.className = 'cat2bug-dsl-preview-table__hint'
    hint.textContent = '加载中…'
    wrapper.append(hint)
    return wrapper
  }

  if (error) {
    const hint = document.createElement('div')
    hint.className = 'cat2bug-dsl-preview-table__hint cat2bug-dsl-preview-table__hint--error'
    hint.textContent = '预览数据加载失败'
    wrapper.append(hint)
    return wrapper
  }

  if (!rows.length) {
    const hint = document.createElement('div')
    hint.className = 'cat2bug-dsl-preview-table__hint'
    hint.textContent = attrs.dataSource === 'api.case.list' ? '暂无测试用例' : '暂无缺陷数据'
    wrapper.append(hint)
    return wrapper
  }

  const scroll = document.createElement('div')
  scroll.className = 'cat2bug-dsl-preview-table__scroll'

  const table = document.createElement('table')
  table.className = 'cat2bug-dsl-preview-table'

  const thead = document.createElement('thead')
  const headRow = document.createElement('tr')
  columns.forEach((col) => {
    const th = document.createElement('th')
    th.textContent = columnHeader(col)
    headRow.append(th)
  })
  thead.append(headRow)
  table.append(thead)

  const tbody = document.createElement('tbody')
  rows.forEach((row) => {
    const tr = document.createElement('tr')
    columns.forEach((col) => {
      const td = document.createElement('td')
      const raw = row[col.field]
      td.textContent = formatCell(raw)
      if (raw != null && String(raw).length > 120) {
        td.title = String(raw)
      }
      tr.append(td)
    })
    tbody.append(tr)
  })
  table.append(tbody)
  scroll.append(table)
  wrapper.append(scroll)

  const meta = document.createElement('div')
  meta.className = 'cat2bug-dsl-preview-table__meta'
  meta.textContent = `共 ${rows.length} 条`
  wrapper.append(meta)

  return wrapper
}
