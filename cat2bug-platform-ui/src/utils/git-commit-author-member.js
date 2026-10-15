/**
 * 按 git commit author 匹配项目成员（与 browse / commit-detail 一致）。
 */

/**
 * @param {{ authorName?: string, authorEmail?: string } | null | undefined} commit
 * @param {Array<object>} members
 * @param {{ id?: number|string, name?: string, nickName?: string, avatar?: string } | null} [self]
 * @returns {object}
 */
export function memberForGitCommitAuthor(commit, members, self) {
  const name = (commit && commit.authorName ? String(commit.authorName) : '').trim()
  const email = (commit && commit.authorEmail ? String(commit.authorEmail) : '').trim()
  const nameKey = name.toLowerCase()
  const emailKey = email.toLowerCase()
  const list = Array.isArray(members) ? members : []
  const matched = list.find((m) => {
    if (!m) return false
    const userName = String(m.userName || '').trim().toLowerCase()
    const nickName = String(m.nickName || '').trim().toLowerCase()
    const mail = String(m.email || '').trim().toLowerCase()
    return (nameKey && (nameKey === userName || nameKey === nickName))
      || (emailKey && mail && emailKey === mail)
  })
  if (matched) return matched
  if (self) {
    const selfName = String(self.name || '').trim().toLowerCase()
    const selfNick = String(self.nickName || '').trim().toLowerCase()
    if (nameKey && ((selfName && nameKey === selfName) || (selfNick && nameKey === selfNick))) {
      return {
        userName: self.name,
        nickName: self.nickName || self.name,
        avatar: self.avatar
      }
    }
  }
  return {
    nickName: name || '-',
    userName: name || '-',
    email: email || undefined
  }
}
