import { resolveToolContent } from '@/components/ReportTemplateEditor/composables/useReportTemplateTools'
import { roundTripTemplate } from '@/components/ReportTemplateEditor/dsl'

const ALIGNMENT_TOOLS = [
  { name: 'font-left', content: '\n:-- {0}\n', marker: ':-- 示例文字' },
  { name: 'font-center', content: '\n-:- {0}\n', marker: '-:- 示例文字' },
  { name: 'font-right', content: '\n--: {0}\n', marker: '--: 示例文字' }
]

const EXTENDED_TOOLS = [
  { name: 'font-underline', content: '++{0}++', marker: '++示例文字++' },
  { name: 'font-strikethrough', content: '~~{0}~~', marker: '~~示例文字~~' },
  { name: 'flag', content: '=={0}==', marker: '==示例文字==' },
  { name: 'font-superscript', content: '^{0}^', marker: '^示例文字^' },
  { name: 'font-subscript', content: '~{0}~', marker: '~示例文字~' }
]

function resolvedSample(tool, sample = '示例文字') {
  const content = resolveToolContent({ ...tool, name: sample })
  expect(content).toBeTruthy()
  return content
}

describe('report template toolbar insert markers', () => {
  it.each(ALIGNMENT_TOOLS)('alignment tool $name inserts expected marker', (tool) => {
    const content = resolvedSample(tool)
    expect(content).toContain(tool.marker)
    const wrapped = `\n${content}\n`
    expect(roundTripTemplate(wrapped)).toBe(wrapped)
  })

  it.each(EXTENDED_TOOLS)('extended markdown tool $name round-trips', (tool) => {
    const content = resolvedSample(tool)
    expect(content).toContain(tool.marker)
    const wrapped = `段落 ${content} 结束`
    expect(roundTripTemplate(wrapped)).toBe(wrapped)
  })
})
