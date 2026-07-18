/** @jest-environment jsdom */

import { renderPreviewCaseCard } from '@/components/ReportTemplateEditor/milkdown/render-preview-case-card'
import {
  getReportPreviewState,
  setReportPreviewData
} from '@/components/ReportTemplateEditor/milkdown/report-preview-data'

describe('renderPreviewCaseCard', () => {
  afterEach(() => {
    setReportPreviewData(null, null, false, null)
  })

  it('returns a wrapper with empty hint when no rows', () => {
    const el = renderPreviewCaseCard({ dataSource: 'api.case.list', raw: '$card{api.case.list}' })
    expect(el.classList.contains('cat2bug-dsl-preview-card-wrap')).toBe(true)
    expect(el.textContent).toContain('暂无测试用例')
  })

  it('renders case rows when preview data is available', () => {
    setReportPreviewData(
      1,
      {
        case: {
          list: [
            { caseNum: 1, caseName: '登录成功' },
            { caseNum: 2, caseName: '登录失败' }
          ]
        }
      },
      false,
      null
    )
    expect(getReportPreviewState().data).toBeTruthy()
    const el = renderPreviewCaseCard({ dataSource: 'api.case.list' })
    expect(el.textContent).toContain('登录成功')
    expect(el.textContent).toContain('#1')
    expect(el.textContent).toContain('共 2 条')
  })
})
