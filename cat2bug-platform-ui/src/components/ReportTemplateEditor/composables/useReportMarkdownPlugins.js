import multimdTable from 'markdown-it-multimd-table'
import { TablePlugin } from '@/components/Cat2BugMarkdown/plugins/TablePlugin'
import { CardPlugin } from '@/components/Cat2BugMarkdown/plugins/CardPlugin'
import { CaseCardPlugin } from '@/components/Cat2BugMarkdown/plugins/CaseCardPlugin'
import { VariablePlugin } from '@/components/Cat2BugMarkdown/plugins/VariablePlugin'
import { PlanVariablePlugin } from '@/components/Cat2BugMarkdown/plugins/PlanVariablePlugin'
import { ImagePlugin } from '@/components/Cat2BugMarkdown/plugins/ImagePlugin'
import { LeftPointPlugin } from '@/components/Cat2BugMarkdown/plugins/LeftPointPlugin'
import { RightPointPlugin } from '@/components/Cat2BugMarkdown/plugins/RightPointPlugin'
import { CenterPointPlugin } from '@/components/Cat2BugMarkdown/plugins/CenterPointPlugin'
import { listDefect } from '@/api/system/defect'
import { getProject } from '@/api/system/project'
import { markdownData } from '@/api/markdown/markdown'

const DEFECT_TABLE_TITLES = [
  'projectNum', 'defectTypeName', 'defectLevel', 'defectName', 'defectStateName',
  'moduleName', 'moduleVersion', 'defectDescribe', 'createBy', 'createTime',
  'updateBy', 'updateTime', 'imgList', 'annexList'
]

const CASE_TABLE_TITLES = [
  'caseNum', 'caseName', 'moduleName', 'casePreconditions', 'caseExpect',
  'caseStep', 'createBy', 'createTime', 'updateBy', 'updateTime'
]

/**
 * Register Cat2Bug markdown-it plugins on a MarkdownRenderer ref (same as Cat2BugMarkdown preview).
 * @param {import('vue').ComponentPublicInstance} markdownViewRef
 * @param {number|string} projectId
 */
export async function setupReportMarkdownPlugins(markdownViewRef, projectId) {
  if (!markdownViewRef || typeof markdownViewRef.use !== 'function') {
    return
  }

  markdownViewRef.use(multimdTable, {
    multiline: true,
    rowspan: true,
    headerless: true,
    multibody: true,
    aotolabel: true
  })
  markdownViewRef.use(LeftPointPlugin)
  markdownViewRef.use(RightPointPlugin)
  markdownViewRef.use(CenterPointPlugin)

  const defectList = listDefect().then((res) => {
    markdownViewRef.use(CardPlugin, {
      name: 'api.defect.list',
      value: res.rows
    })
    return res
  })

  const project = getProject(projectId).then((res) => {
    markdownViewRef.use(ImagePlugin, {
      name: 'api.project',
      value: res.data
    })
    markdownViewRef.use(VariablePlugin, {
      name: 'api.project',
      value: res.data
    })
    return res
  })

  const mdData = markdownData(projectId).then((res) => {
    markdownViewRef.use(TablePlugin, {
      name: 'api.defect.list',
      value: res.data.defect ? res.data.defect.list : [],
      defaultTitles: DEFECT_TABLE_TITLES
    })
    markdownViewRef.use(TablePlugin, {
      name: 'api.case.list',
      value: res.data.case ? res.data.case.list : [],
      defaultTitles: CASE_TABLE_TITLES
    })
    markdownViewRef.use(CaseCardPlugin, {
      name: 'api.case.list',
      value: res.data.case ? res.data.case.list : []
    })
    markdownViewRef.use(VariablePlugin, {
      name: 'api.case.total',
      value: res.data.case ? res.data.case.total : 0
    })
    markdownViewRef.use(PlanVariablePlugin, {
      name: 'api.plan',
      value: res.data.plan ? res.data.plan.list : []
    })
    return res
  })

  await Promise.all([defectList, project, mdData])
}
