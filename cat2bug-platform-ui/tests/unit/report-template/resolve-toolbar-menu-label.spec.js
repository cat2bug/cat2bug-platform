import i18n from '@/utils/i18n/i18n'
import { resolveToolbarMenuLabel } from '@/components/Cat2BugMarkdown/resolveToolbarMenuLabel'

describe('resolveToolbarMenuLabel', () => {
  it('resolves flat keys from default i18n export (project.name)', () => {
    expect(resolveToolbarMenuLabel(i18n, 'project.name')).toBe('项目名称')
    expect(resolveToolbarMenuLabel(i18n, 'project.icon')).toBe('项目封面')
    expect(resolveToolbarMenuLabel(i18n, 'create-time')).toBe('创建时间')
  })

  it('resolves keys that conflict with parent string messages (case.total)', () => {
    expect(resolveToolbarMenuLabel(i18n, 'case.total')).toBe('测试用例总数')
  })

  it('resolves business root tab labels', () => {
    expect(resolveToolbarMenuLabel(i18n, 'project')).toBe('项目')
    expect(resolveToolbarMenuLabel(i18n, 'case')).toBe('测试用例')
    expect(resolveToolbarMenuLabel(i18n, 'defect')).toBe('缺陷')
    expect(resolveToolbarMenuLabel(i18n, 'plan')).toBe('测试计划')
  })
})
