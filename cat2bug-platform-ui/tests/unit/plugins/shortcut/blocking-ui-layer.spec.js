/** @jest-environment jsdom */

import { hasBlockingUiLayer } from '@/plugins/shortcut/service'

function mountVisibleCaseDrawer() {
  document.body.innerHTML = `
    <div class="el-drawer__wrapper" style="position:fixed;inset:0;width:100px;height:100px;">
      <div class="el-drawer rtl" style="width:50%;height:100%;">
        <div class="case-add-header">新建用例</div>
      </div>
    </div>
  `
}

describe('hasBlockingUiLayer', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
  })

  it('blocks page shortcuts when case form drawer header is visible', () => {
    mountVisibleCaseDrawer()
    expect(hasBlockingUiLayer()).toBe(true)
  })

  it('does not block Esc close path when form drawer excluded', () => {
    mountVisibleCaseDrawer()
    expect(hasBlockingUiLayer({
      excludeDefectFormDrawer: true,
      excludeHandleDefectDrawer: true,
      excludeDefectToolDialog: true
    })).toBe(false)
  })

  it('does not block Esc close path when AI case drawer excluded as form drawer', () => {
    document.body.innerHTML = `
      <div class="el-drawer__wrapper" style="position:fixed;inset:0;width:100px;height:100px;">
        <div class="el-drawer rtl" style="width:50%;height:100%;">
          <div class="case-search-header">AI用例生成</div>
        </div>
      </div>
    `
    expect(hasBlockingUiLayer()).toBe(true)
    expect(hasBlockingUiLayer({
      excludeDefectFormDrawer: true,
      excludeHandleDefectDrawer: true,
      excludeDefectToolDialog: true
    })).toBe(false)
  })

  it('does not block Esc close path when case import dialog excluded', () => {
    document.body.innerHTML = `
      <div class="el-dialog__wrapper" style="position:fixed;inset:0;width:100px;height:100px;">
        <div class="el-dialog case-import-dialog" style="width:400px;height:200px;">
          <div class="el-dialog__body"><div class="el-upload"></div></div>
        </div>
      </div>
    `
    expect(hasBlockingUiLayer()).toBe(true)
    expect(hasBlockingUiLayer({ excludeCaseImportDialog: true })).toBe(false)
  })

  it('does not block Esc close path when view report drawer excluded', () => {
    document.body.innerHTML = `
      <div class="el-drawer__wrapper" style="position:fixed;inset:0;width:100px;height:100px;">
        <div class="el-drawer rtl report-drawer-accent" style="width:50%;height:100%;">
          <div class="report-edit-header">报告详情</div>
        </div>
      </div>
    `
    expect(hasBlockingUiLayer()).toBe(true)
    expect(hasBlockingUiLayer({
      excludeDefectFormDrawer: true,
      excludeHandleDefectDrawer: true,
      excludeViewReportDrawer: true,
      excludeDefectToolDialog: true
    })).toBe(false)
  })

  it('does not block Esc close path when ModuleDialog excluded as form shortcut dialog', () => {
    document.body.innerHTML = `
      <div class="el-dialog__wrapper" style="position:fixed;inset:0;width:100px;height:100px;">
        <div class="el-dialog" style="width:400px;height:200px;">
          <div class="el-dialog__body"></div>
        </div>
      </div>
    `
    const wrapper = document.querySelector('.el-dialog__wrapper')
    wrapper.__vue__ = { $options: { name: 'ModuleDialog' }, $parent: null }
    expect(hasBlockingUiLayer()).toBe(true)
    expect(hasBlockingUiLayer({
      excludeDefectFormDrawer: true,
      excludeHandleDefectDrawer: true,
      excludeDefectToolDialog: true
    })).toBe(false)
  })

  it('does not block Esc close path when cat2bug-form-shortcut-dialog class is present', () => {
    document.body.innerHTML = `
      <div class="el-dialog__wrapper" style="position:fixed;inset:0;width:100px;height:100px;">
        <div class="el-dialog cat2bug-form-shortcut-dialog" style="width:400px;height:200px;">
          <div class="el-dialog__body"></div>
        </div>
      </div>
    `
    expect(hasBlockingUiLayer()).toBe(true)
    expect(hasBlockingUiLayer({
      excludeDefectFormDrawer: true,
      excludeHandleDefectDrawer: true,
      excludeDefectToolDialog: true
    })).toBe(false)
  })

  it('blocks page Esc when project icon popover is open', () => {
    document.body.innerHTML = `
      <div class="el-popover project-icon-popper" style="position:fixed;width:550px;height:300px;">
        <div class="project-icon-popper"></div>
      </div>
    `
    expect(hasBlockingUiLayer()).toBe(true)
    expect(hasBlockingUiLayer({
      excludeDefectFormDrawer: true,
      excludeHandleDefectDrawer: true,
      excludeDefectToolDialog: true
    })).toBe(true)
  })
})
