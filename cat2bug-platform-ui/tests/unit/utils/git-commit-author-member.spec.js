import { memberForGitCommitAuthor } from '@/utils/git-commit-author-member'

describe('memberForGitCommitAuthor', () => {
  const members = [
    { userId: 1, userName: 'alice', nickName: 'Alice', email: 'alice@example.com', avatar: '/a.png' },
    { userId: 2, userName: 'bob', nickName: 'Bob', email: 'bob@example.com' }
  ]

  it('matches by email', () => {
    const m = memberForGitCommitAuthor(
      { authorName: 'Other', authorEmail: 'alice@example.com' },
      members
    )
    expect(m.userId).toBe(1)
  })

  it('matches by nickName', () => {
    const m = memberForGitCommitAuthor({ authorName: 'Bob', authorEmail: '' }, members)
    expect(m.userId).toBe(2)
  })

  it('returns placeholder when unmatched', () => {
    const m = memberForGitCommitAuthor(
      { authorName: 'Nobody', authorEmail: 'x@y.z' },
      members
    )
    expect(m.nickName).toBe('Nobody')
    expect(m.userId).toBeUndefined()
  })
})
