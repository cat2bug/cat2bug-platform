import { buildPlanStatisticsTable } from '@/components/ReportTemplateEditor/composables/planToolMenus'
import {
  isPlanStatisticsTableContent,
  isMultimdMergedTableContent,
  findRawPreservedBlockRanges,
  splitSourceWithRawBlocks,
  roundTripTemplate,
  parseTemplate
} from '@/components/ReportTemplateEditor/dsl'
import fs from 'fs'
import path from 'path'

const multimdFixture = fs.readFileSync(
  path.join(__dirname, '../../fixtures/report-template/phase2-multimd-merge-sample.md'),
  'utf8'
)

describe('report template complex raw blocks', () => {
  it('detects plan statistics mega-table', () => {
    const table = buildPlanStatisticsTable(2)
    expect(isPlanStatisticsTableContent(table)).toBe(true)
  })

  it('extracts plan statistics as raw block range', () => {
    const table = buildPlanStatisticsTable(1)
    const wrapped = `# 报告\n\n${table}\n\n## 结尾`
    const ranges = findRawPreservedBlockRanges(wrapped)
    expect(ranges).toHaveLength(1)
    expect(ranges[0].kind).toBe('plan-statistics')
    expect(ranges[0].value).toContain('${api.plan.planName[1]}')
    expect(ranges[0].value).toContain('${api.plan.defectRepairAvgHour[1]}')
  })

  it('plan statistics table round-trips via raw segment', () => {
    const table = buildPlanStatisticsTable(3)
    const segments = parseTemplate(table)
    expect(segments.some((segment) => segment.type === 'raw')).toBe(true)
    expect(roundTripTemplate(table)).toBe(table)
  })

  it('detects multimd merged table sample', () => {
    expect(isMultimdMergedTableContent(multimdFixture)).toBe(true)
  })

  it('multimd merge sample round-trips', () => {
    expect(roundTripTemplate(multimdFixture.trimEnd())).toBe(multimdFixture.trimEnd())
  })

  it('splitSourceWithRawBlocks preserves markdown around raw regions', () => {
    const table = buildPlanStatisticsTable(1)
    const source = `前言\n\n${table}\n\n后记`
    const parts = splitSourceWithRawBlocks(source)
    expect(parts.filter((part) => part.type === 'raw')).toHaveLength(1)
    expect(parts[0].value).toBe('前言\n\n')
    expect(parts[parts.length - 1].value.trim()).toBe('后记')
  })
})
