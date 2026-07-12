import i18n from '@/utils/i18n/i18n'
import { strFormat } from '@/utils'
import {
  DEFECT_TABLE_DEFAULT_COLUMNS,
  DEFECT_TABLE_TOOLBAR_TOKEN,
  parseTemplate
} from '../dsl'

/**
 * Resolve tool content string (same rules as Cat2BugMarkdown.insertContent).
 * @param {object} tool
 */
export function resolveToolContent(tool) {
  let content = tool.content
  if (content instanceof Function || content instanceof Promise) {
    return null
  }
  const hasI18nKey = typeof i18n.te === 'function'
    ? i18n.te(content)
    : i18n.global?.te?.(content)
  if (hasI18nKey) {
    content = i18n.t(content)
  }
  if (typeof content === 'string' && content.includes('{0}')) {
    content = strFormat(content, tool.name)
  }
  return content
}

/**
 * Append tool insert content as parsed segments.
 * @param {import('../dsl/parse').TemplateSegment[]} segments
 * @param {string} content
 */
export function appendToolContent(segments, content) {
  if (!content) {
    return segments
  }
  const prefix = segments.length && !endsWithNewline(segments) ? [{ type: 'text', value: '\n' }] : []
  const parsed = parseTemplate(`\n${content}\n`)
  return [...segments, ...prefix, ...parsed]
}

function endsWithNewline(segments) {
  if (!segments.length) {
    return false
  }
  const last = segments[segments.length - 1]
  if (last.type === 'text') {
    return (last.value || '').endsWith('\n')
  }
  return true
}

export function useReportTemplateTools() {
  function getStaticInsertActions() {
    return {
      projectName: {
        label: i18n.t('project.name'),
        apply(segments) {
          return appendToolContent(segments, `${i18n.t('project.name')}: \${api.project.projectName}`)
        }
      },
      projectIcon: {
        label: i18n.t('project.icon'),
        apply(segments) {
          return appendToolContent(
            segments,
            `$image["style":"width:200px;height:200px;"]{api.project.projectIcon}`
          )
        }
      },
      caseTotal: {
        label: i18n.t('case.total'),
        apply(segments) {
          return appendToolContent(segments, `${i18n.t('case.total')}: \${api.case.total}`)
        }
      },
      caseCard: {
        label: i18n.t('case.card'),
        apply(segments) {
          return appendToolContent(segments, '$card{api.case.list}')
        }
      },
      defectTable: {
        label: i18n.t('defect.table'),
        apply(segments) {
          return appendToolContent(segments, DEFECT_TABLE_TOOLBAR_TOKEN)
        }
      }
    }
  }

  function createDefectTableSegment(columns) {
    return {
      type: 'defectTable',
      dataSource: 'api.defect.list',
      columns: columns || DEFECT_TABLE_DEFAULT_COLUMNS.map((field) => ({ field, label: null })),
      raw: DEFECT_TABLE_TOOLBAR_TOKEN
    }
  }

  function createCaseCardSegment() {
    return {
      type: 'caseCard',
      dataSource: 'api.case.list',
      raw: '$card{api.case.list}'
    }
  }

  function createDynamicImageSegment(path = 'api.project.projectIcon', style = 'width:200px;height:200px;') {
    const raw = `$image["style":"${style}"]{${path}}`
    return {
      type: 'dynamicImage',
      style,
      path,
      raw
    }
  }

  function createPlanVariableSegment(path, planIndex) {
    return {
      type: 'planVariable',
      path,
      planIndex,
      raw: `\${${path}[${planIndex}]}`
    }
  }

  return {
    resolveToolContent,
    appendToolContent,
    getStaticInsertActions,
    createDefectTableSegment,
    createCaseCardSegment,
    createDynamicImageSegment,
    createPlanVariableSegment
  }
}
