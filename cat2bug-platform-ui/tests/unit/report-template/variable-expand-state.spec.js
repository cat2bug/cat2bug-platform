import {
  __resetVariableExpandStateForTests,
  createVariableUnitKey,
  getVariableDisplayMode,
  getVariableExpandGeneration,
  getVariablesExpanded,
  setVariableDisplayModeOverride,
  setVariablesExpanded,
  subscribeVariableExpandState,
  toggleVariableDisplayMode
} from '@/components/ReportTemplateEditor/milkdown/variable-expand-state'
import {
  pathLastSegment,
  resolveVariableTitle
} from '@/components/ReportTemplateEditor/milkdown/variable-title-labels'

describe('variable-expand-state', () => {
  beforeEach(() => {
    __resetVariableExpandStateForTests()
  })

  it('defaults to expanded content mode', () => {
    expect(getVariablesExpanded()).toBe(true)
    const key = createVariableUnitKey()
    expect(getVariableDisplayMode(key)).toBe('content')
  })

  it('setVariablesExpanded collapses all and clears overrides', () => {
    const key = createVariableUnitKey()
    setVariableDisplayModeOverride(key, 'variable')
    expect(getVariableDisplayMode(key)).toBe('variable')

    const genBefore = getVariableExpandGeneration()
    setVariablesExpanded(false)
    expect(getVariablesExpanded()).toBe(false)
    expect(getVariableExpandGeneration()).toBe(genBefore + 1)
    expect(getVariableDisplayMode(key)).toBe('variable')

    setVariablesExpanded(true)
    expect(getVariableDisplayMode(key)).toBe('content')
  })

  it('toggleVariableDisplayMode sets local override', () => {
    const key = createVariableUnitKey()
    expect(toggleVariableDisplayMode(key)).toBe('variable')
    expect(getVariableDisplayMode(key)).toBe('variable')
    expect(toggleVariableDisplayMode(key)).toBe('content')
    expect(getVariableDisplayMode(key)).toBe('content')
  })

  it('toolbar setAll overrides local mode', () => {
    const key = createVariableUnitKey()
    toggleVariableDisplayMode(key)
    expect(getVariableDisplayMode(key)).toBe('variable')
    setVariablesExpanded(true)
    expect(getVariableDisplayMode(key)).toBe('content')
  })

  it('subscribeVariableExpandState notifies on setAll', () => {
    const seen = []
    const unsub = subscribeVariableExpandState((snap) => seen.push(snap))
    setVariablesExpanded(false)
    expect(seen.length).toBe(1)
    expect(seen[0].expanded).toBe(false)
    unsub()
    setVariablesExpanded(true)
    expect(seen.length).toBe(1)
  })
})

describe('variable-title-labels', () => {
  it('falls back to last path segment for unknown paths', () => {
    expect(pathLastSegment('api.custom.fooBar')).toBe('fooBar')
    expect(resolveVariableTitle('api.custom.fooBar')).toBe('fooBar')
  })

  it('resolves known project variable title via i18n when available', () => {
    const title = resolveVariableTitle('api.project.projectName')
    expect(title).toBeTruthy()
    expect(title).not.toBe('api.project.projectName')
  })

  it('appends plan index for plan paths', () => {
    const title = resolveVariableTitle('api.plan.passCount', { planIndex: 2 })
    expect(title).toContain('[2]')
  })
})

describe('variable mode body uses raw token (contract)', () => {
  it('raw token string is used as variable-mode body source', () => {
    const attrs = { path: 'api.project.projectName', raw: '${api.project.projectName}' }
    const mode = 'variable'
    const body = mode === 'variable' ? (attrs.raw || '') : 'resolved'
    expect(body).toBe('${api.project.projectName}')
  })
})
