import fs from 'fs'
import path from 'path'
import { roundTripTemplate } from '@/components/ReportTemplateEditor/dsl'

const fixturesDir = path.join(__dirname, '../../fixtures/report-template')

describe('report template ViewReport / export regression fixtures', () => {
  const goldenFiles = [
    'phase0-sample.md',
    'phase0-e4-viewreport-golden.md',
    'phase2-multimd-merge-sample.md'
  ]

  it.each(goldenFiles)('%s round-trips without DSL corruption', (filename) => {
    const content = fs.readFileSync(path.join(fixturesDir, filename), 'utf8')
    expect(roundTripTemplate(content.trimEnd())).toBe(content.trimEnd())
  })

  it('project export report_template.json entry shape is unchanged', () => {
    const body = '# 标题\n\n$table{api.defect.list[projectNum,defectName]}'
    const entry = {
      templateTitle: '回归模版',
      templateContent: body,
      templateIconUrl: null,
      majorVersion: 1,
      minorVersion: 0
    }
    expect(typeof entry.templateContent).toBe('string')
    expect(entry.templateContent).toContain('$table{api.defect.list')
    expect(roundTripTemplate(entry.templateContent)).toBe(entry.templateContent)
  })
})
