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
import {
  POPOVER_SELECTORS,
  closePopover,
  isPopoverComponentVm,
  isPopoverOpen,
  isVisiblePopperEl,
  openPopover,
  queryVisiblePoppersByClass,
  resolvePopoverComponent
} from '@/adapters/element-plus/popover-adapter'
import {
  SELECT_SELECTORS,
  closeSelect,
  isSelectComponentVm,
  isSelectOpen,
  querySelectDropdownItems,
  resolveSelectComponent
} from '@/adapters/element-plus/select-adapter'
import {
  TABLE_SELECTORS,
  findTableBodyRowFromActiveElement,
  getElTableStore
} from '@/adapters/element-plus/table-adapter'

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

describe('element-plus popover adapter', () => {
  it('exposes stable selectors', () => {
    expect(POPOVER_SELECTORS.root).toBe('.el-popover')
  })

  it('recognizes ElPopover vm and open/close', () => {
    const popoverVm = {
      $options: { name: 'ElPopover' },
      showPopper: false,
      doShow() { this.showPopper = true },
      doClose() { this.showPopper = false }
    }
    expect(isPopoverComponentVm(popoverVm)).toBe(true)
    expect(openPopover(popoverVm)).toBe(true)
    expect(isPopoverOpen(popoverVm)).toBe(true)
    expect(closePopover(popoverVm)).toBe(true)
    expect(isPopoverOpen(popoverVm)).toBe(false)
  })

  it('resolves popover vm from trigger __vue__ chain', () => {
    const popoverVm = { $options: { name: 'ElPopover' }, showPopper: false }
    const btn = document.createElement('button')
    btn.__vue__ = { $parent: popoverVm }
    document.body.appendChild(btn)
    expect(resolvePopoverComponent(btn)).toBe(popoverVm)
    btn.remove()
  })

  it('queries visible poppers by class', () => {
    const hidden = document.createElement('div')
    hidden.className = 'defect-column-picker-popover'
    hidden.style.display = 'none'
    const visible = document.createElement('div')
    visible.className = 'defect-column-picker-popover'
    visible.getBoundingClientRect = () => ({ width: 10, height: 10 })
    document.body.append(hidden, visible)
    expect(queryVisiblePoppersByClass('defect-column-picker-popover')).toEqual([visible])
    expect(isVisiblePopperEl(visible)).toBe(true)
    hidden.remove()
    visible.remove()
  })
})

describe('element-plus select adapter', () => {
  it('exposes stable selectors', () => {
    expect(SELECT_SELECTORS.root).toBe('.el-select')
  })

  it('recognizes ElSelect vm and close', () => {
    const selectVm = {
      $options: { name: 'ElSelect' },
      visible: true,
      handleClose() { this.visible = false }
    }
    expect(isSelectComponentVm(selectVm)).toBe(true)
    expect(isSelectOpen(selectVm)).toBe(true)
    expect(closeSelect(selectVm)).toBe(true)
    expect(isSelectOpen(selectVm)).toBe(false)
  })

  it('resolves select from .el-select root', () => {
    const selectVm = { $options: { name: 'ElSelect' }, visible: false, $refs: {} }
    const root = document.createElement('div')
    root.className = 'el-select'
    root.__vue__ = selectVm
    document.body.appendChild(root)
    expect(resolveSelectComponent(root)).toBe(selectVm)
    root.remove()
  })

  it('reads dropdown items from popper element', () => {
    const dropdown = document.createElement('div')
    dropdown.innerHTML = '<li class="el-select-dropdown__item"></li><li class="el-select-dropdown__item is-disabled"></li>'
    document.body.appendChild(dropdown)
    expect(querySelectDropdownItems(dropdown)).toHaveLength(1)
    dropdown.remove()
  })
})

describe('element-plus table adapter', () => {
  it('exposes stable selectors', () => {
    expect(TABLE_SELECTORS.bodyRow).toBe('.el-table__body tbody tr')
  })

  it('finds body row from active cell', () => {
    const wrapper = document.createElement('div')
    wrapper.className = 'el-table__body'
    const tbody = document.createElement('tbody')
    const tr = document.createElement('tr')
    const td = document.createElement('td')
    tr.appendChild(td)
    tbody.appendChild(tr)
    wrapper.appendChild(tbody)
    document.body.appendChild(wrapper)
    td.focus()
    expect(findTableBodyRowFromActiveElement(td)).toBe(tr)
    wrapper.remove()
  })

  it('reads el-table store ref', () => {
    const store = { states: {} }
    expect(getElTableStore({ store })).toBe(store)
  })
})
