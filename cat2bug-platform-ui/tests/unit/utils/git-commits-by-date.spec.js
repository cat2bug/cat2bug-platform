import { groupCommitsByDate } from '@/utils/git-commits-by-date'

describe('groupCommitsByDate', () => {
  it('groups by local calendar day newest first and preserves order within group', () => {
    const commits = [
      { id: 'a', commitTime: '2024-06-20T10:00:00' },
      { id: 'b', commitTime: '2024-06-20T08:00:00' },
      { id: 'c', commitTime: '2024-06-15T12:00:00' }
    ]
    const groups = groupCommitsByDate(commits, 'en-US')
    expect(groups).toHaveLength(2)
    expect(groups[0].dateKey).toBe('2024-06-20')
    expect(groups[0].items.map(c => c.id)).toEqual(['a', 'b'])
    expect(groups[1].dateKey).toBe('2024-06-15')
    expect(groups[1].items.map(c => c.id)).toEqual(['c'])
    expect(groups[0].label).toBeTruthy()
  })

  it('returns empty for empty input', () => {
    expect(groupCommitsByDate([])).toEqual([])
    expect(groupCommitsByDate(null)).toEqual([])
  })
})
