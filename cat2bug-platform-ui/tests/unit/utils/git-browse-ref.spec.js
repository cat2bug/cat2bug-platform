import {
  applyBrowseRefState,
  detachedBrowseRefOption,
  pickBrowseDisplayBranch,
  pickBrowseRef,
  resolveBrowseBranchName,
  resolveBrowseCurrentRef
} from '@/utils/git-browse-ref'

describe('git-browse-ref', () => {
  const branches = [
    { name: 'feature-x' },
    { name: 'develop' },
    { name: 'core', default: true }
  ]

  it('prefers query.ref over default branch (incl. commit SHA)', () => {
    expect(pickBrowseRef('d447900abcdef', branches)).toBe('d447900abcdef')
    expect(pickBrowseRef('develop', branches)).toBe('develop')
  })

  it('falls back to default branch when query.ref empty', () => {
    expect(pickBrowseRef('', branches)).toBe('core')
    expect(pickBrowseRef(null, branches)).toBe('core')
  })

  it('uses query.branch as currentRef when query.ref is absent', () => {
    expect(resolveBrowseCurrentRef(null, 'feature-x', branches)).toBe('feature-x')
    expect(resolveBrowseCurrentRef('', 'develop', branches)).toBe('develop')
  })

  it('builds detached option for commit SHA not in branch list', () => {
    expect(detachedBrowseRefOption('d447900abcdef1234', branches)).toEqual({
      value: 'd447900abcdef1234',
      label: 'd447900'
    })
    expect(detachedBrowseRefOption('core', branches)).toBe(null)
  })

  it('picks display branch from query.branch while browsing a commit', () => {
    expect(pickBrowseDisplayBranch('develop', branches)).toBe('develop')
    expect(pickBrowseDisplayBranch('missing', branches)).toBe('core')
    expect(pickBrowseDisplayBranch(null, branches)).toBe('core')
  })

  it('resolves browse branch from candidates then default', () => {
    expect(resolveBrowseBranchName(['missing', 'develop'], branches)).toBe('develop')
    expect(resolveBrowseBranchName([null, ''], branches)).toBe('core')
    expect(resolveBrowseBranchName([], [])).toBe(null)
  })

  it('keeps explicit candidate when branch list is not loaded yet', () => {
    expect(resolveBrowseBranchName(['feature-x', 'core'], [])).toBe('feature-x')
  })

  it('preserves query.branch across commit-detail remount (no query.ref)', () => {
    const state = applyBrowseRefState({ branch: 'feature-x' }, branches, null)
    expect(state.currentRef).toBe('feature-x')
    expect(state.selectedBranchName).toBe('feature-x')
  })

  it('keeps selected branch when browsing at commit SHA', () => {
    const state = applyBrowseRefState(
      { ref: 'abc123def4567890', branch: 'feature-x' },
      branches,
      null
    )
    expect(state.currentRef).toBe('abc123def4567890')
    expect(state.selectedBranchName).toBe('feature-x')
  })

  it('does not overwrite query.branch with default when remounting commit detail', () => {
    const state = applyBrowseRefState({ branch: 'develop' }, branches, 'develop')
    expect(state.currentRef).toBe('develop')
    expect(state.selectedBranchName).toBe('develop')
  })
})
