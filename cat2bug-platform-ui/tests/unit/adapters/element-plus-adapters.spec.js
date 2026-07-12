/** @jest-environment jsdom */

import {
  DATE_PICKER_SELECTORS,
  isDatePickerEditorEl,
  isDatePickerFocusEl,
  resolveDatePickerComponent
} from '@/adapters/element-plus/date-picker-adapter'
import {
  DROPDOWN_SELECTORS,
  findDropdownTrigger,
  isElDropdownVm,
  resolveDropdownComponent
} from '@/adapters/element-plus/dropdown-adapter'

describe('element-plus date-picker adapter', () => {
  it('exposes stable selectors', () => {
    expect(DATE_PICKER_SELECTORS.editor).toBe('.el-date-editor')
    expect(DATE_PICKER_SELECTORS.panel).toBe('.el-picker-panel')
  })

  it('detects date editor elements', () => {
    const el = document.createElement('div')
    el.className = 'el-date-editor'
    expect(isDatePickerEditorEl(el)).toBe(true)
    expect(isDatePickerFocusEl(el)).toBe(false)
  })

  it('resolves date picker vm from editor __vue__', () => {
    const pickerVm = {
      handleFocus() {},
      pickerVisible: false
    }
    const editor = document.createElement('div')
    editor.className = 'el-date-editor'
    editor.__vue__ = pickerVm
    document.body.appendChild(editor)
    expect(resolveDatePickerComponent(editor)).toBe(pickerVm)
    expect(isDatePickerFocusEl(editor)).toBe(true)
    editor.remove()
  })
})

describe('element-plus dropdown adapter', () => {
  it('exposes stable selectors', () => {
    expect(DROPDOWN_SELECTORS.host).toBe('.el-dropdown')
  })

  it('recognizes ElDropdown component vm', () => {
    expect(isElDropdownVm({ $options: { name: 'ElDropdown' } })).toBe(true)
    expect(isElDropdownVm({ $options: { name: 'ElButton' } })).toBe(false)
  })

  it('resolves dropdown vm and trigger', () => {
    const dropdownVm = { $options: { name: 'ElDropdown' } }
    const host = document.createElement('div')
    host.className = 'el-dropdown'
    host.__vue__ = dropdownVm
    const btn = document.createElement('button')
    host.appendChild(btn)
    document.body.appendChild(host)
    expect(resolveDropdownComponent(host)).toBe(dropdownVm)
    expect(findDropdownTrigger(host)).toBe(btn)
    host.remove()
  })
})
