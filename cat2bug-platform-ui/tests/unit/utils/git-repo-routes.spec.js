import {
  buildGitRepoClonePath,
  buildGitRepoSpaPath,
  gitRepoBrowseRoute,
  gitRepoCommitDetailRoute,
  gitRepoSettingsDangerRoute,
  gitRepoSettingsGeneralRoute,
  gitRepoSettingsMembersRoute,
  gitRepoSettingsRoute,
  gitRepoSettingsWebhooksRoute,
  isGitCloneSlugParam,
  resolveBrowseActiveTab,
  resolveShellTabFromRoute,
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

  it('resolves browse active tab from params and query (incl. releases from PR shell)', () => {
    expect(resolveBrowseActiveTab(null)).toBe('code')
    expect(resolveBrowseActiveTab({ params: {}, query: {} })).toBe('code')
    expect(resolveBrowseActiveTab({ params: {}, query: { tab: 'releases' } })).toBe('releases')
    expect(resolveBrowseActiveTab({ params: {}, query: { tab: 'commits' } })).toBe('commits')
    expect(resolveBrowseActiveTab({ params: { releaseId: '9' }, query: { tab: 'code' } })).toBe('releases')
    expect(resolveBrowseActiveTab({ params: { commitId: 'abc' }, query: {} })).toBe('commits')
  })

  it('builds settings named routes', () => {
    const ctx = { teamKey: 'acme', projectKey: 'demo', slug: 'backend' }
    expect(gitRepoSettingsRoute(ctx)).toEqual({
      name: 'GitRepoSettings',
      params: { teamKey: 'acme', projectKey: 'demo', slug: 'backend' }
    })
    expect(gitRepoSettingsGeneralRoute(ctx)).toEqual({
      name: 'GitRepoSettingsGeneral',
      params: { teamKey: 'acme', projectKey: 'demo', slug: 'backend' }
    })
    expect(gitRepoSettingsDangerRoute(ctx)).toEqual({
      name: 'GitRepoSettingsDanger',
      params: { teamKey: 'acme', projectKey: 'demo', slug: 'backend' }
    })
    expect(gitRepoSettingsMembersRoute(ctx)).toEqual({
      name: 'GitRepoSettingsMembers',
      params: { teamKey: 'acme', projectKey: 'demo', slug: 'backend' }
    })
    expect(gitRepoSettingsWebhooksRoute(ctx)).toEqual({
      name: 'GitRepoSettingsWebhooks',
      params: { teamKey: 'acme', projectKey: 'demo', slug: 'backend' }
    })
  })

  it('resolves shell tab including settings', () => {
    expect(resolveShellTabFromRoute(null)).toBe('code')
    expect(resolveShellTabFromRoute({ name: 'GitRepoSettingsGeneral' })).toBe('settings')
    expect(resolveShellTabFromRoute({ name: 'GitRepoSettingsDanger' })).toBe('settings')
    expect(resolveShellTabFromRoute({ name: 'GitRepoSettingsMembers' })).toBe('settings')
    expect(resolveShellTabFromRoute({ name: 'GitRepoSettingsWebhooks' })).toBe('settings')
    expect(resolveShellTabFromRoute({ name: 'GitRepoPullRequests' })).toBe('pulls')
    expect(resolveShellTabFromRoute({ name: 'GitRepoBrowse', query: { tab: 'releases' } })).toBe('releases')
    expect(resolveShellTabFromRoute({ name: 'GitRepoBrowse', query: {} })).toBe('code')
  })
})
