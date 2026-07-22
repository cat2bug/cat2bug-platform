import {
  buildGitRepoClonePath,
  buildGitRepoSpaPath,
  gitRepoBrowseRoute,
  gitRepoCommitDetailRoute,
  isGitCloneSlugParam,
  spaAndClonePathsDistinct
} from '@/utils/git-repo-routes'

describe('git-repo-routes', () => {
  it('builds SPA path without .git and clone path with .git', () => {
    expect(buildGitRepoSpaPath('acme', 'demo', 'backend')).toBe('/acme/demo/code/backend')
    expect(buildGitRepoClonePath('acme', 'demo', 'backend')).toBe('/acme/demo/code/backend.git')
    expect(spaAndClonePathsDistinct('acme', 'demo', 'backend')).toBe(true)
  })

  it('rejects clone-looking slug for SPA path helpers', () => {
    expect(isGitCloneSlugParam('backend.git')).toBe(true)
    expect(buildGitRepoSpaPath('acme', 'demo', 'backend.git')).toBe('')
    expect(spaAndClonePathsDistinct('acme', 'demo', 'backend.git')).toBe(false)
  })

  it('builds namespaced named routes for browse and commits', () => {
    const ctx = { teamKey: 'acme', projectKey: 'demo', slug: 'backend' }
    expect(gitRepoBrowseRoute(ctx)).toEqual({
      name: 'GitRepoBrowse',
      params: { teamKey: 'acme', projectKey: 'demo', slug: 'backend' }
    })
    expect(gitRepoCommitDetailRoute(ctx, 'abc123')).toEqual({
      name: 'GitRepoCommitDetail',
      params: { teamKey: 'acme', projectKey: 'demo', slug: 'backend', commitId: 'abc123' }
    })
  })
})
