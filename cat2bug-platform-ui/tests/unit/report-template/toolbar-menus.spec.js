import { injectPlanToolMenu, buildPlanStatisticsTable } from '@/components/ReportTemplateEditor/composables/planToolMenus'
import { resolveToolContent, appendToolContent } from '@/components/ReportTemplateEditor/composables/useReportTemplateTools'
import { parseTemplate } from '@/components/ReportTemplateEditor/dsl'

const mockBaseTools = [
  { name: 'case', children: [] },
  { name: 'defect', children: [] }
]

describe('report template toolbar menus', () => {
  it('injects plan menu after case menu', () => {
    const tools = injectPlanToolMenu(mockBaseTools, [
      { planName: 'Sprint 1', planNumber: 1 }
    ])
    const caseIdx = tools.findIndex((t) => t.name === 'case')
    expect(tools[caseIdx + 1].name).toBe('plan')
  })

  it('resolves image insert content', () => {
    const content = resolveToolContent({
      name: 'project.icon',
      content: '$image["style":"width:100px;height:100px;"]{api.project.projectIcon}'
    })
    expect(content).toContain('$image')
    expect(content).toContain('api.project.projectIcon')
  })

  it('appendToolContent parses case card segment', () => {
    const next = appendToolContent([], '$card{api.case.list}')
    const card = next.find((s) => s.type === 'caseCard')
    expect(card).toBeTruthy()
    expect(card.dataSource).toBe('api.case.list')
  })

  it('plan statistics content parses plan variables', () => {
    const table = buildPlanStatisticsTable(1)
    const segments = parseTemplate(table)
    const planVars = segments.filter((s) => s.type === 'planVariable')
    expect(planVars.length).toBeGreaterThan(5)
  })
})
