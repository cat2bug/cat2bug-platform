import { buildPlanStatisticsTable } from '@/components/ReportTemplateEditor/composables/planToolMenus'
import {
  isPlanStatisticsTableContent,
  isMultimdMergedTableContent,
  findRawPreservedBlockRanges,
  splitSourceWithRawBlocks,
  normalizePlanStatisticsTablesInSource,
  roundTripTemplate
} from '@/components/ReportTemplateEditor/dsl'
import { normalizePipeTableMarkdown } from '@/components/ReportTemplateEditor/dsl/normalize-pipe-table'
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

  it('does not preserve plan statistics as raw block (Milkdown GFM table instead)', () => {
    const table = buildPlanStatisticsTable(1)
    const wrapped = `# 报告\n\n${table}\n\n## 结尾`
    const ranges = findRawPreservedBlockRanges(wrapped)
    expect(ranges.every((range) => range.kind !== 'plan-statistics')).toBe(true)
    expect(ranges).toHaveLength(0)
  })

  it('normalizes legacy plan statistics into GFM with a header separator', () => {
    // Use unnormalized builder input shape by reconstructing from a known legacy fragment
    // that still ends with defectRepairAvgHour (range detector key).
    const legacy = `|项目名称|\${api.project.projectName}|||||||||||||||||
|测试计划|\${api.plan.planName[1]}|||||||||||||||||
|度量指标数据如下:||||||||||||||||||
|\${api.plan.defectDiscoveryRate[1]}|||||||||||||||\${api.plan.defectRepairAvgHour[1]}h||`
    const normalized = normalizePipeTableMarkdown(legacy)
    const lines = normalized.split('\n')
    expect(lines[1]).toMatch(/^\|(-\|)+$/)
    expect(normalized).toContain('${api.plan.defectRepairAvgHour[1]}')
    const inDoc = normalizePlanStatisticsTablesInSource(`前言\n\n${legacy}\n\n后记`)
    expect(inDoc).toContain('前言')
    expect(inDoc.split('\n').some((line) => /^\|(-\|)+$/.test(line))).toBe(true)
  })

  it('plan statistics table round-trips as markdown text (not raw segment)', () => {
    const table = buildPlanStatisticsTable(3)
    expect(roundTripTemplate(table)).toContain('${api.plan.planName[3]}')
    expect(roundTripTemplate(table)).toContain('度量指标数据如下')
  })

  it('detects multimd rowspan sample with ^^', () => {
    expect(isMultimdMergedTableContent(multimdFixture)).toBe(true)
  })

  it('does not treat GFM empty cells as multimd', () => {
    const gfm = `|a|b|c|\n|-|-|-|\n|x||z|`
    expect(isMultimdMergedTableContent(gfm)).toBe(false)
  })

  it('no longer preserves multimd tables as raw blocks (editable instead)', () => {
    const ranges = findRawPreservedBlockRanges(multimdFixture)
    expect(ranges).toHaveLength(0)
    const parts = splitSourceWithRawBlocks(`前言\n\n${multimdFixture}\n\n后记`)
    expect(parts.filter((part) => part.type === 'raw')).toHaveLength(0)
  })

  it('multimd rowspan sample round-trips', () => {
    expect(roundTripTemplate(multimdFixture.trimEnd())).toBe(multimdFixture.trimEnd())
  })

  it('splitSourceWithRawBlocks keeps plan statistics in markdown parts', () => {
    const table = buildPlanStatisticsTable(1)
    const source = `前言\n\n${table}\n\n后记`
    const parts = splitSourceWithRawBlocks(source)
    expect(parts.filter((part) => part.type === 'raw')).toHaveLength(0)
    expect(parts.map((part) => part.value).join('')).toContain('前言')
    expect(parts.map((part) => part.value).join('')).toContain('后记')
  })
})
