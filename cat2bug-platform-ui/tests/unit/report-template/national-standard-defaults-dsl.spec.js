import fs from 'fs'
import path from 'path'
import {
  parseTemplate,
  PLAN_VARIABLE_FIELDS,
  PROJECT_VARIABLE_FIELDS,
  DEFECT_TABLE_DEFAULT_COLUMNS,
  CASE_TABLE_DEFAULT_COLUMNS
} from '@/components/ReportTemplateEditor/dsl'

const defaultsDir = path.join(
  __dirname,
  '../../../../readme/development/skills/report-templates/defaults'
)

const DEFAULT_FILES = [
  'test-status-report.md',
  'test-completion-report.md',
  'test-plan.md',
  'test-incident-report.md',
  'test-specification.md',
  'test-data-readiness-report.md',
  'test-environment-readiness-report.md',
  'skeleton-test-policy.md',
  'skeleton-organizational-test-practices.md'
]

const VARIABLE_RE = /\$\{((?:\w|\.)+)\}/g
const PLAN_VARIABLE_RE = /\$\{((?:\w|\.)+)\[(\d+)\]\}/g
const TABLE_RE = /\$table\{((?:\w|\.)+)(?:\[([^\]]*)\])?\}/g
const IMAGE_RE = /\$image(?:\[[^\]]*\])?\{((?:\w|\.)+)\}/g
const CARD_RE = /\$card\{((?:\w|\.)+)\}/g

const PROJECT_SET = new Set(PROJECT_VARIABLE_FIELDS)
const PLAN_SET = new Set(PLAN_VARIABLE_FIELDS)
const DEFECT_COL_SET = new Set(DEFECT_TABLE_DEFAULT_COLUMNS)
const CASE_COL_SET = new Set(CASE_TABLE_DEFAULT_COLUMNS)

function assertKnownTokens(source, fileName) {
  const issues = []

  let m
  PLAN_VARIABLE_RE.lastIndex = 0
  while ((m = PLAN_VARIABLE_RE.exec(source))) {
    const pathName = m[1]
    const idx = Number(m[2])
    if (!pathName.startsWith('api.plan.')) {
      issues.push(`plan path must start with api.plan.: ${m[0]}`)
      continue
    }
    const field = pathName.slice('api.plan.'.length)
    if (!PLAN_SET.has(field)) {
      issues.push(`unknown plan field ${field} in ${m[0]}`)
    }
    if (!Number.isInteger(idx) || idx < 1) {
      issues.push(`plan index must be 1-based integer: ${m[0]}`)
    }
  }

  // Strip plan vars so simple VARIABLE_RE does not double-hit them
  const withoutPlan = source.replace(PLAN_VARIABLE_RE, '')
  VARIABLE_RE.lastIndex = 0
  while ((m = VARIABLE_RE.exec(withoutPlan))) {
    const p = m[1]
    if (p === 'api.case.total') continue
    if (p.startsWith('api.project.')) {
      const field = p.slice('api.project.'.length)
      if (!PROJECT_SET.has(field)) {
        issues.push(`unknown project field ${field} in \${${p}}`)
      }
      continue
    }
    issues.push(`unsupported simple variable \${${p}}`)
  }

  TABLE_RE.lastIndex = 0
  while ((m = TABLE_RE.exec(source))) {
    const ds = m[1]
    const colsRaw = m[2]
    if (ds !== 'api.defect.list' && ds !== 'api.case.list') {
      issues.push(`unknown table dataSource ${ds}`)
      continue
    }
    if (colsRaw == null || colsRaw.trim() === '') continue
    const allowed = ds === 'api.defect.list' ? DEFECT_COL_SET : CASE_COL_SET
    for (const part of colsRaw.split(',')) {
      const field = part.trim().split(':')[0]
      if (!field) continue
      if (!allowed.has(field)) {
        issues.push(`unknown column ${field} for ${ds}`)
      }
    }
  }

  IMAGE_RE.lastIndex = 0
  while ((m = IMAGE_RE.exec(source))) {
    if (m[1] !== 'api.project.projectIcon') {
      issues.push(`unsupported image path ${m[1]}`)
    }
  }

  CARD_RE.lastIndex = 0
  while ((m = CARD_RE.exec(source))) {
    if (m[1] !== 'api.case.list') {
      issues.push(`unsupported card dataSource ${m[1]}`)
    }
  }

  if (issues.length) {
    throw new Error(`${fileName}:\n- ${issues.join('\n- ')}`)
  }
}

describe('national-standard default templates DSL smoke', () => {
  it('defaults directory exists with expected files', () => {
    expect(fs.existsSync(defaultsDir)).toBe(true)
    for (const name of DEFAULT_FILES) {
      expect(fs.existsSync(path.join(defaultsDir, name))).toBe(true)
    }
  })

  it.each(DEFAULT_FILES)('parses tokens in %s without inventing fields', (name) => {
    const source = fs.readFileSync(path.join(defaultsDir, name), 'utf8')
    expect(() => parseTemplate(source)).not.toThrow()
    const segments = parseTemplate(source)
    expect(Array.isArray(segments)).toBe(true)
    assertKnownTokens(source, name)
  })
})
