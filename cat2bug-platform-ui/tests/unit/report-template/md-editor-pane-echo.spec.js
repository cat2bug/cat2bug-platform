/** @jest-environment jsdom */
import { normalizeDslSource } from '@/components/ReportTemplateEditor/dsl/normalize'

/**
 * Mirrors MdEditorPane onChange ignore logic for split-pane echo suppression.
 * While ignoreChangeValue is set, both matching and non-matching echoes are dropped
 * (Codemirror may emit intermediate strings during prop sync).
 */
function shouldIgnoreSourceEcho(ignoreChangeValue, nextValue) {
  if (ignoreChangeValue === null) {
    return false
  }
  return true
}

function shouldEmitSourceChange(ignoreChangeValue, nextValue, modelValue) {
  if (shouldIgnoreSourceEcho(ignoreChangeValue, nextValue)) {
    return false
  }
  return normalizeDslSource(nextValue) !== normalizeDslSource(modelValue)
}

describe('MdEditorPane split-pane echo suppression', () => {
  it('ignores onChange while echo window is open (exact match)', () => {
    const pushed = '描述：${api.project.projectIntroduce}\n\n'
    const ignoreChangeValue = normalizeDslSource(pushed)
    expect(shouldEmitSourceChange(ignoreChangeValue, pushed, pushed)).toBe(false)
  })

  it('ignores onChange while echo window is open even if Codemirror differs', () => {
    const pushed = '|a||\n|-|-|\n||b|'
    const ignoreChangeValue = normalizeDslSource(pushed)
    const cmEcho = '|a| <br /> |\n|-|-|\n| <br /> |b|'
    expect(shouldEmitSourceChange(ignoreChangeValue, cmEcho, pushed)).toBe(false)
  })

  it('emits when ignore window is cleared and user edits source', () => {
    const model = '描述：${api.project.projectIntroduce}\n'
    const edited = '描述：${api.project.projectIntroduce}\n\n新行\n'
    expect(shouldEmitSourceChange(null, edited, model)).toBe(true)
  })

  it('does not emit when value is normalized-equivalent to model', () => {
    expect(shouldEmitSourceChange(null, '行$1\n', '行\\$1\n')).toBe(false)
  })
})
