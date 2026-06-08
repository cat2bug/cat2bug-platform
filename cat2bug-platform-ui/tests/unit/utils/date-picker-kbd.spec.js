/** @jest-environment jsdom */

import {
  clearRangePanelFocusVisual,
  ensureDateVisibleOnRangePanel,
  getDateTableBodyRows,
  handleActiveDateRangeKeydown,
  handleDateRangePanelKeydown,
  isDatePickerPanelOpen,
  isRangeDatePicker,
  moveRangeFocusDateByKey,
  syncRangePanelFocusVisual,
  wrapDatePickerKeyboard
} from '@/utils/date-picker-kbd'

describe('date-picker-kbd daterange', () => {
  it('moveRangeFocusDateByKey moves by day and week', () => {
    const start = new Date(2026, 5, 10)
    expect(moveRangeFocusDateByKey(start, 39, null).getDate()).toBe(11)
    expect(moveRangeFocusDateByKey(start, 37, null).getDate()).toBe(9)
    expect(moveRangeFocusDateByKey(start, 40, null).getDate()).toBe(17)
    expect(moveRangeFocusDateByKey(start, 38, null).getDate()).toBe(3)
  })

  it('moveRangeFocusDateByKey skips disabled dates', () => {
    const start = new Date(2026, 5, 10)
    const disabled = (d) => d.getDate() === 11
    const next = moveRangeFocusDateByKey(start, 39, disabled)
    expect(next.getDate()).toBe(12)
  })

  it('ensureDateVisibleOnRangePanel scrolls linked calendars', () => {
    const panel = {
      unlinkPanels: false,
      leftDate: new Date(2026, 0, 1),
      rightDate: new Date(2026, 1, 1)
    }
    ensureDateVisibleOnRangePanel(panel, new Date(2026, 5, 15))
    expect(panel.leftDate.getMonth()).toBe(5)
    expect(panel.rightDate.getMonth()).toBe(6)
  })

  it('handleDateRangePanelKeydown picks start then end with Enter', () => {
    const panel = {
      leftDate: new Date(2026, 5, 1),
      rightDate: new Date(2026, 6, 1),
      unlinkPanels: false,
      minDate: null,
      maxDate: null,
      rangeState: { selecting: false },
      disabledDate: null,
      handleChangeRange: jest.fn(),
      handleRangePick: jest.fn()
    }
    const pickerVm = {
      ranged: true,
      pickerVisible: true,
      picker: panel
    }
    panel._cat2bugKbdFocusDate = new Date(2026, 5, 8)
    handleDateRangePanelKeydown(pickerVm, { keyCode: 13, target: {} })
    expect(panel.handleRangePick).toHaveBeenCalledWith(
      { minDate: panel._cat2bugKbdFocusDate, maxDate: null },
      false
    )
    expect(panel.rangeState.selecting).toBe(true)

    panel.minDate = new Date(2026, 5, 8)
    panel._cat2bugKbdFocusDate = new Date(2026, 5, 20)
    handleDateRangePanelKeydown(pickerVm, { keyCode: 32, target: {} })
    expect(panel.handleRangePick).toHaveBeenLastCalledWith(
      { minDate: panel.minDate, maxDate: panel._cat2bugKbdFocusDate },
      true
    )
  })

  it('getDateTableBodyRows skips empty leading tbody tr', () => {
    document.body.innerHTML = `
      <table class="el-date-table">
        <tbody>
          <tr></tr>
          <tr><td><div><span>5</span></div></td></tr>
          <tr><td><div><span>12</span></div></td></tr>
        </tbody>
      </table>`
    const rows = getDateTableBodyRows(document.querySelector('.el-date-table'))
    expect(rows).toHaveLength(2)
  })

  it('syncRangePanelFocusVisual aligns rows when tbody has empty first tr', () => {
    document.body.innerHTML = `
      <div class="panel">
        <table class="el-date-table">
          <tbody>
            <tr></tr>
            <tr>
              <td><div><span>5</span></div></td>
              <td><div><span>6</span></div></td>
              <td><div><span>7</span></div></td>
              <td><div><span>8</span></div></td>
            </tr>
            <tr>
              <td><div><span>12</span></div></td>
              <td><div><span>13</span></div></td>
              <td><div><span>14</span></div></td>
              <td><div><span>15</span></div></td>
            </tr>
          </tbody>
        </table>
      </div>`
    const tableEl = document.querySelector('.el-date-table')
    const trs = getDateTableBodyRows(tableEl)
    const panel = {
      $el: document.querySelector('.panel'),
      leftDate: new Date(2026, 6, 1),
      rightDate: new Date(2026, 7, 1)
    }
    const tableVm = {
      $el: tableEl,
      rows: [
        [{ type: 'normal' }, { type: 'normal' }, { type: 'normal' }, { type: 'normal' }],
        [{ type: 'normal' }, { type: 'normal' }, { type: 'normal' }, { type: 'normal' }]
      ],
      getDateOfCell(r, c) {
        if (r === 0 && c === 3) return new Date(2026, 6, 8)
        if (r === 1 && c === 3) return new Date(2026, 6, 15)
        return null
      }
    }
    panel.$children = [tableVm]
    syncRangePanelFocusVisual(panel, new Date(2026, 6, 15))
    expect(trs[1].querySelectorAll('td')[3].classList.contains('cat2bug-date-kbd-current')).toBe(true)
    expect(trs[0].querySelectorAll('td')[3].classList.contains('cat2bug-date-kbd-current')).toBe(false)
  })

  it('isRangeDatePicker detects daterange type', () => {
    expect(isRangeDatePicker({ type: 'daterange' })).toBe(true)
    expect(isRangeDatePicker({ type: 'date' })).toBe(false)
  })

  it('isDatePickerPanelOpen detects active range editor', () => {
    document.body.innerHTML = '<div class="el-date-editor el-range-editor is-active"></div>'
    const editor = document.querySelector('.el-range-editor')
    editor.__vue__ = { pickerVisible: true, handleFocus() {}, type: 'daterange' }
    expect(isDatePickerPanelOpen()).toBe(true)
  })
})
