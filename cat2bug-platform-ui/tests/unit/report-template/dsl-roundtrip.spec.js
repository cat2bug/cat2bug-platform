import fs from 'fs'
import path from 'path'
import { buildPlanStatisticsTable } from '@/components/ReportTemplateEditor/composables/planToolMenus'
import { PLAN_VARIABLE_FIELDS } from '@/components/ReportTemplateEditor/dsl/grammar'
import {
  DEFECT_TABLE_TOOLBAR_TOKEN,
  DEFECT_TABLE_DEFAULT_COLUMNS,
  roundTripTemplate,
  parseTemplate,
  serializeTemplate
} from '@/components/ReportTemplateEditor/dsl'

const fixturePath = path.join(__dirname, '../../fixtures/report-template/phase0-sample.md')
const phase0Sample = fs.readFileSync(fixturePath, 'utf8')

describe('report template DSL round-trip', () => {
  it('preserves project variable', () => {
    const input = '项目名称: ${api.project.projectName}'
    expect(roundTripTemplate(input)).toBe(input)
  })

  it('preserves case total variable', () => {
    const input = '用例总数: ${api.case.total}'
    expect(roundTripTemplate(input)).toBe(input)
  })

  it('preserves plan variable with index', () => {
    const input = '缺陷总数: ${api.plan.defectCount[2]}'
    expect(roundTripTemplate(input)).toBe(input)
  })

  it('preserves default defect table token', () => {
    expect(roundTripTemplate(DEFECT_TABLE_TOOLBAR_TOKEN)).toBe(DEFECT_TABLE_TOOLBAR_TOKEN)
  })

  it('preserves defect table with column aliases', () => {
    const input = '$table{api.defect.list[defectName:缺陷名称,defectLevel:等级]}'
    expect(roundTripTemplate(input)).toBe(input)
  })

  it('preserves case card token', () => {
    const input = '$card{api.case.list}'
    expect(roundTripTemplate(input)).toBe(input)
  })

  it('preserves dynamic image token', () => {
    const input = '$image["style":"width:200px;height:200px;"]{api.project.projectIcon}'
    expect(roundTripTemplate(input)).toBe(input)
  })

  it('preserves image style variants', () => {
    const input = '$image["style":"width:100px;height:100px;"]{api.project.projectIcon}'
    expect(roundTripTemplate(input)).toBe(input)
  })

  it('preserves composite sample template', () => {
    const input = [
      '# 测试报告',
      '',
      '项目名称: ${api.project.projectName}',
      DEFECT_TABLE_TOOLBAR_TOKEN,
      '',
      '缺陷总数: ${api.plan.defectCount[1]}'
    ].join('\n')
    expect(roundTripTemplate(input)).toBe(input)
  })

  it('updates defect table columns on serialize', () => {
    const segments = parseTemplate(DEFECT_TABLE_TOOLBAR_TOKEN)
    expect(segments).toHaveLength(1)
    segments[0].columns = [
      { field: 'projectNum', label: null },
      { field: 'defectName', label: '名称' }
    ]
    expect(serializeTemplate(segments)).toBe(
      '$table{api.defect.list[projectNum,defectName:名称]}'
    )
  })

  it('default columns match toolbar', () => {
    const segments = parseTemplate(DEFECT_TABLE_TOOLBAR_TOKEN)
    expect(segments[0].columns.map((c) => c.field)).toEqual(DEFECT_TABLE_DEFAULT_COLUMNS)
  })

  it('phase0 golden sample round-trips', () => {
    expect(roundTripTemplate(phase0Sample)).toBe(phase0Sample)
  })

  it('phase0 sample defect table column edit serializes correctly', () => {
    const segments = parseTemplate(phase0Sample)
    const tableIdx = segments.findIndex((s) => s.type === 'defectTable')
    expect(tableIdx).toBeGreaterThan(-1)
    segments[tableIdx].columns = [
      { field: 'projectNum', label: null },
      { field: 'defectName', label: null }
    ]
    const output = serializeTemplate(segments)
    expect(output).toContain('$table{api.defect.list[projectNum,defectName]}')
    expect(roundTripTemplate(output)).toBe(output)
  })

  it('plan statistics table preserves plan variables', () => {
    const table = buildPlanStatisticsTable(1)
    expect(table).toContain('${api.plan.planName[1]}')
    expect(table).toContain('${api.plan.defectRepairRate[1]}')
    expect(roundTripTemplate(table)).toBe(table)
  })

  it('samples plan variable fields round-trip', () => {
    const samples = ['defectCount', 'passCount', 'defectRepairAvgHour'].map(
      (field) => `x: \${api.plan.${field}[3]}`
    )
    samples.forEach((input) => {
      expect(roundTripTemplate(input)).toBe(input)
    })
    expect(PLAN_VARIABLE_FIELDS.length).toBeGreaterThanOrEqual(30)
  })
})
