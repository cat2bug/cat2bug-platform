import { emitPreservedRawMarkdown } from '@/components/ReportTemplateEditor/milkdown/raw-block-serialize'
import { buildPlanStatisticsTable } from '@/components/ReportTemplateEditor/composables/planToolMenus'

describe('plan-statistics raw block markdown serialization', () => {
  it('emitPreservedRawMarkdown uses a single html node (avoids break → \\\\n accumulation)', () => {
    const calls = []
    const state = {
      addNode: (...args) => {
        calls.push(args)
        return state
      }
    }
    const table = buildPlanStatisticsTable(1)
    emitPreservedRawMarkdown(state, table)
    expect(calls).toHaveLength(1)
    expect(calls[0][0]).toBe('html')
    expect(calls[0][2]).toBe(table)
    expect(calls.some((c) => c[0] === 'break')).toBe(false)
    expect(calls.some((c) => c[0] === 'text')).toBe(false)
  })

  it('emitPreservedRawMarkdown no-ops on empty value', () => {
    const calls = []
    const state = { addNode: (...args) => { calls.push(args); return state } }
    emitPreservedRawMarkdown(state, '')
    expect(calls).toEqual([])
  })
})
