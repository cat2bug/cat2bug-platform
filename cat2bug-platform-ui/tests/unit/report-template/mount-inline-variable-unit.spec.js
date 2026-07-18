/** @jest-environment jsdom */

import { mountInlineVariableUnit } from '@/components/ReportTemplateEditor/milkdown/mount-inline-variable-unit'
import { setReportPreviewData } from '@/components/ReportTemplateEditor/milkdown/report-preview-data'
import {
  __resetVariableExpandStateForTests,
  setVariablesExpanded
} from '@/components/ReportTemplateEditor/milkdown/variable-expand-state'

describe('mount-inline-variable-unit', () => {
  afterEach(() => {
    __resetVariableExpandStateForTests()
    setReportPreviewData(null, null, false, null)
  })

  it('shows resolved content by default and raw token after toggle', () => {
    setReportPreviewData(1, { project: { projectName: 'Demo' } }, false, null)
    setVariablesExpanded(true)

    const unit = mountInlineVariableUnit({
      path: 'api.project.projectName',
      raw: '${api.project.projectName}'
    })

    expect(unit.dom.classList.contains('is-content')).toBe(true)
    expect(unit.dom.querySelector('.cat2bug-dsl-var__body').textContent).toBe('Demo')

    unit.dom.querySelector('.cat2bug-dsl-var__toggle').click()
    expect(unit.dom.classList.contains('is-variable')).toBe(true)
    expect(unit.dom.querySelector('.cat2bug-dsl-var__body').textContent).toBe('${api.project.projectName}')

    unit.destroy()
  })

  it('adds is-hovered on mouseenter', () => {
    const unit = mountInlineVariableUnit({
      path: 'api.project.projectName',
      raw: '${api.project.projectName}'
    })
    unit.dom.dispatchEvent(new MouseEvent('mouseenter', { bubbles: true }))
    expect(unit.dom.classList.contains('is-hovered')).toBe(true)
    unit.destroy()
  })
})
