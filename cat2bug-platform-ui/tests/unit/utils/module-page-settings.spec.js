import {
  mergeModulePageSettings,
  toModulePageSettingsPayload,
  effectiveDocumentUploadMb,
  buildLabelOverrideMessages,
  teamModuleKeyFromLocation,
  isTeamSettingsPath,
  filterMenuRoutesByTeamModules,
  omitTeamGatedDefectColumns,
  hiddenDefectBuiltinFormKeys,
  isMcpKeyClosedByTeam,
  DEFAULT_SYSTEM_MAX_UPLOAD_MB
} from '@/utils/module-page-settings'

describe('module-page-settings', () => {
  it('merges camelCase casePlan and system max aliases', () => {
    const merged = mergeModulePageSettings({
      casePlan: { enabled: false, labelCase: ' 用例单 ', labelPlan: '' },
      document: { maxUploadMb: '12', systemMaxMb: 80 }
    })
    expect(merged.case_plan.enabled).toBe(false)
    expect(merged.case_plan.labelCase).toBe('用例单')
    expect(merged.case_plan.labelPlan).toBeNull()
    expect(merged.document.maxUploadMb).toBe(12)
    expect(merged.systemMaxUploadMb).toBe(80)
    expect(merged.defect.enabled).toBe(true)
  })

  it('payload drops the read-only system cap', () => {
    const payload = toModulePageSettingsPayload({
      systemMaxUploadMb: 80,
      code: { enabled: false, label: '仓库' }
    })
    expect(payload.systemMaxUploadMb).toBeUndefined()
    expect(payload.code).toEqual({ enabled: false, label: '仓库' })
    expect(payload.case_plan.enabled).toBe(true)
  })

  it('uses team upload limit, then system max, then 500', () => {
    expect(effectiveDocumentUploadMb({ document: { maxUploadMb: 20 }, systemMax: 80 })).toBe(20)
    expect(effectiveDocumentUploadMb({ systemMaxUploadMb: 80 })).toBe(80)
    expect(effectiveDocumentUploadMb(null)).toBe(DEFAULT_SYSTEM_MAX_UPLOAD_MB)
  })

  it('overrides root keys after noun replacement', () => {
    const next = buildLabelOverrideMessages({
      case: '测试用例',
      plan: '测试计划',
      defect: '缺陷',
      'defect.manage': '缺陷管理',
      module: '交付物',
      'case.add': '新建测试用例',
      planTime: '计划时间'
    }, {
      case_plan: { labelCase: '用例单', labelPlan: '排期' },
      defect: { label: '问题单' },
      deliverable: { label: '模块' }
    }, 'zh_CN')
    expect(next.case).toBe('用例单')
    expect(next.plan).toBe('排期')
    expect(next.defect).toBe('问题单')
    expect(next['defect.manage']).toBe('问题单')
    expect(next.module).toBe('模块')
    expect(next['case.add']).toBe('新建用例单')
    expect(next.planTime).toBe('计划时间')
  })

  it('maps project routes and never gates team settings', () => {
    expect(teamModuleKeyFromLocation({ path: '/acme/proj/case' })).toBe('case_plan')
    expect(teamModuleKeyFromLocation({ path: '/acme/proj/module/1' })).toBe('deliverable')
    expect(teamModuleKeyFromLocation({ path: '/project/git-repo/files' })).toBe('code')
    expect(teamModuleKeyFromLocation({ path: '/team-options/team-option' })).toBeNull()
    expect(isTeamSettingsPath('/team/project-list')).toBe(true)
  })

  it('filters closed sidebar modules but keeps hidden team routes', () => {
    const settings = { deliverable: { enabled: false }, case_plan: { enabled: false } }
    const routes = [
      { path: '/team-options/team-option', hidden: true, children: [{ path: 'case' }] },
      {
        path: '/acme/proj',
        children: [
          { path: '/acme/proj/defect', meta: { projectModule: 'defect' } },
          { path: '/acme/proj/case', meta: { projectModule: 'case' } },
          { path: '/acme/proj/module', meta: { projectModule: 'module' } }
        ]
      }
    ]
    const filtered = filterMenuRoutesByTeamModules(routes, settings)
    expect(filtered[0].children).toHaveLength(1)
    expect(filtered[1].children.map(item => item.meta.projectModule)).toEqual(['defect'])
  })

  it('hides case column and form keys, and grays closed MCP keys', () => {
    const settings = { case_plan: { enabled: false }, deliverable: { enabled: false } }
    expect(hiddenDefectBuiltinFormKeys(settings)).toEqual(['caseId', 'moduleId'])
    expect(omitTeamGatedDefectColumns([
      { key: 'case' },
      { prop: 'caseId' },
      { key: 'module' },
      { prop: 'moduleId' },
      { key: 'name' }
    ], settings)).toEqual([{ key: 'name' }])
    expect(isMcpKeyClosedByTeam(settings, 'plan')).toBe(true)
    expect(isMcpKeyClosedByTeam(settings, 'dashboard')).toBe(false)
  })
})
