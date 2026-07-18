/** @jest-environment jsdom */

import { buildPlanStatisticsTable } from '@/components/ReportTemplateEditor/composables/planToolMenus'
import {
  classifyPlanStatisticsCell,
  disposePlanStatisticsPreview,
  parsePipeTableRows,
  renderPreviewPlanStatistics,
  splitPipeTableCells,
  tokenizeTableCell
} from '@/components/ReportTemplateEditor/milkdown/render-preview-plan-statistics'
import { setReportPreviewData } from '@/components/ReportTemplateEditor/milkdown/report-preview-data'
import {
  __resetVariableExpandStateForTests,
  setVariablesExpanded
} from '@/components/ReportTemplateEditor/milkdown/variable-expand-state'

describe('render-preview-plan-statistics', () => {
  afterEach(() => {
    __resetVariableExpandStateForTests()
    setReportPreviewData(null, null, false, null)
  })

  it('splits pipe cells and skips separator rows', () => {
    expect(splitPipeTableCells('|a|b|')).toEqual(['a', 'b'])
    const rows = parsePipeTableRows('|a|b|\n|-|-|\n|c|d|')
    expect(rows).toEqual([['a', 'b'], ['c', 'd']])
  })

  it('tokenizes plan and project variables in a cell', () => {
    const tokens = tokenizeTableCell('名称${api.project.projectName}/${api.plan.passCount[2]}个')
    expect(tokens).toEqual([
      { type: 'text', value: '名称' },
      { type: 'variable', path: 'api.project.projectName', raw: '${api.project.projectName}' },
      { type: 'text', value: '/' },
      {
        type: 'planVariable',
        path: 'api.plan.passCount',
        planIndex: 2,
        raw: '${api.plan.passCount[2]}'
      },
      { type: 'text', value: '个' }
    ])
  })

  it('renders an HTML table with resolved variable values in content mode', () => {
    setReportPreviewData(
      1,
      {
        project: { projectName: 'Cat2Bug-Platform' },
        plan: {
          list: [{ planNumber: 1, planName: 'Sprint-1', passCount: 12, defectCount: 3 }]
        }
      },
      false,
      null
    )
    setVariablesExpanded(true)
    const table = buildPlanStatisticsTable(1)
    const el = renderPreviewPlanStatistics(table)
    expect(el.querySelector('table')).toBeTruthy()
    expect(el.textContent).toContain('Cat2Bug-Platform')
    expect(el.textContent).toContain('Sprint-1')
    expect(el.textContent).not.toContain('${api.project.projectName}')
    disposePlanStatisticsPreview(el)
  })

  it('renders raw tokens in variable mode', () => {
    setVariablesExpanded(false)
    const table = buildPlanStatisticsTable(1)
    const el = renderPreviewPlanStatistics(table)
    expect(el.textContent).toContain('${api.project.projectName}')
    expect(el.textContent).toContain('${api.plan.planName[1]}')
    disposePlanStatisticsPreview(el)
  })

  it('classifies plain title cells as label and variable cells as value', () => {
    expect(classifyPlanStatisticsCell(tokenizeTableCell('项目名称'))).toBe('label')
    expect(classifyPlanStatisticsCell(tokenizeTableCell('${api.project.projectName}'))).toBe('value')
    expect(classifyPlanStatisticsCell(tokenizeTableCell(''))).toBe('empty')
    expect(classifyPlanStatisticsCell(tokenizeTableCell('${api.plan.passCount[1]} 个'))).toBe('value')
  })

  it('marks label and value cells with distinct classes', () => {
    const el = renderPreviewPlanStatistics('|项目名称|${api.project.projectName}|\n|-|-|')
    const cells = el.querySelectorAll('td')
    expect(cells[0].className).toContain('cat2bug-dsl-preview-plan-stats__cell--label')
    expect(cells[1].className).toContain('cat2bug-dsl-preview-plan-stats__cell--value')
    disposePlanStatisticsPreview(el)
  })

  it('mounts interactive variable units with hover chrome structure', () => {
    const el = renderPreviewPlanStatistics('|项目名称|${api.project.projectName}|\n|-|-|')
    const unit = el.querySelector('.cat2bug-dsl-var__view')
    expect(unit).toBeTruthy()
    expect(unit.querySelector('.cat2bug-dsl-var__frame')).toBeTruthy()
    expect(unit.querySelector('.cat2bug-dsl-var__chrome')).toBeTruthy()
    expect(unit.querySelector('.cat2bug-dsl-var__toggle')).toBeTruthy()
    expect(unit.querySelector('.cat2bug-dsl-var__title')).toBeTruthy()
    expect(unit.querySelector('.cat2bug-dsl-var__body')).toBeTruthy()

    unit.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true }))
    expect(unit.classList.contains('is-hovered')).toBe(true)

    disposePlanStatisticsPreview(el)
  })
})
