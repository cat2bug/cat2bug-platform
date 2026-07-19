import {
  filterReportContentToolbarTools,
  REPORT_CONTENT_EXCLUDED_TOOL_NAMES
} from '@/components/Report/reportContentToolbarFilter'

describe('reportContentToolbarFilter', () => {
  const sampleTools = [
    { type: 'siding' },
    { name: 'font-weight', content: '**{0}**' },
    { name: 'table', content: '|a|' },
    { type: 'siding', name: 'siding-9899' },
    { name: 'project', children: [] },
    { name: 'case', children: [] },
    { name: 'defect', children: [] },
    { type: 'siding' },
    { name: 'clear', type: 'command' },
    { name: 'save', type: 'command' },
    { type: 'siding' }
  ]

  it('excludes project/case/defect variable tools', () => {
    const tools = filterReportContentToolbarTools(sampleTools)
    const names = tools.map((t) => t.name).filter(Boolean)
    ;['project', 'case', 'defect'].forEach((excluded) => {
      expect(names).not.toContain(excluded)
      expect(REPORT_CONTENT_EXCLUDED_TOOL_NAMES.has(excluded)).toBe(true)
    })
  })

  it('keeps static markdown formatting and save/clear', () => {
    const tools = filterReportContentToolbarTools(sampleTools)
    const names = new Set(tools.map((t) => t.name).filter(Boolean))
    expect(names.has('font-weight')).toBe(true)
    expect(names.has('table')).toBe(true)
    expect(names.has('save')).toBe(true)
    expect(names.has('clear')).toBe(true)
  })

  it('does not leave leading/trailing or double sidings', () => {
    const tools = filterReportContentToolbarTools(sampleTools)
    expect(tools[0] && tools[0].type).not.toBe('siding')
    expect(tools[tools.length - 1] && tools[tools.length - 1].type).not.toBe('siding')
    for (let i = 1; i < tools.length; i++) {
      if (tools[i].type === 'siding' && tools[i - 1].type === 'siding') {
        throw new Error('adjacent sidings')
      }
    }
  })
})
