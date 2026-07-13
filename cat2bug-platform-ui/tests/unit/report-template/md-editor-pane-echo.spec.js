/** @jest-environment jsdom */
import { normalizeDslSource } from '@/components/ReportTemplateEditor/dsl/normalize'

/**
 * Mirrors MdEditorPane onChange ignore logic for split-pane echo suppression.
 */
function shouldIgnoreSourceEcho(ignoreChangeValue, nextValue) {
  if (ignoreChangeValue === null) {
    return false
  }
  return normalizeDslSource(nextValue) === normalizeDslSource(ignoreChangeValue)
}

describe('MdEditorPane split-pane echo suppression', () => {
  it('ignores onChange that echoes the value just pushed from visual editor', () => {
    const pushed = '描述：${api.project.projectIntroduce}\n\n'
    const ignoreChangeValue = normalizeDslSource(pushed)
    expect(shouldIgnoreSourceEcho(ignoreChangeValue, pushed)).toBe(true)
  })

  it('does not ignore when user edits source to different content', () => {
    const pushed = '描述：${api.project.projectIntroduce}\n'
    const ignoreChangeValue = normalizeDslSource(pushed)
    const edited = '描述：${api.project.projectIntroduce}\n\n新行\n'
    expect(shouldIgnoreSourceEcho(ignoreChangeValue, edited)).toBe(false)
  })

  it('treats normalized-equivalent strings as echo', () => {
    const pushed = '行\\$1\n'
    const ignoreChangeValue = normalizeDslSource(pushed)
    expect(shouldIgnoreSourceEcho(ignoreChangeValue, '行$1\n')).toBe(true)
  })
})
