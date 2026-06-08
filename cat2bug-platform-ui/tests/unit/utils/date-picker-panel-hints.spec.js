/** @jest-environment jsdom */

import {
  DATE_PANEL_NAV_KEYS,
  hideDatePickerPanelHints,
  installDatePickerPanelHints,
  runDatePanelNavByKeyCode,
  showDatePickerPanelHints
} from '@/utils/date-picker-panel-hints'

describe('date-picker-panel-hints', () => {
  beforeEach(() => {
    document.body.innerHTML = ''
    hideDatePickerPanelHints()
  })

  it('exports nav key labels', () => {
    expect(DATE_PANEL_NAV_KEYS.prevYear).toBe('-')
    expect(DATE_PANEL_NAV_KEYS.nextYear).toBe('=')
  })

  it('runDatePanelNavByKeyCode invokes panel method', () => {
    const panel = {
      $el: document.createElement('div'),
      leftPrevYear: jest.fn(),
      leftPrevMonth: jest.fn(),
      rightNextMonth: jest.fn(),
      rightNextYear: jest.fn()
    }
    expect(runDatePanelNavByKeyCode(189, panel)).toBe(true)
    expect(panel.leftPrevYear).toHaveBeenCalled()
    expect(runDatePanelNavByKeyCode(null, panel, '-')).toBe(true)
  })

  it('showDatePickerPanelHints renders badges on header buttons', () => {
    document.body.innerHTML = `
      <div class="el-picker-panel el-date-range-picker">
        <div class="el-date-range-picker__content is-left">
          <div class="el-date-range-picker__header">
            <button type="button" class="el-picker-panel__icon-btn el-icon-d-arrow-left"></button>
            <button type="button" class="el-picker-panel__icon-btn el-icon-arrow-left"></button>
          </div>
        </div>
        <div class="el-date-range-picker__content is-right">
          <div class="el-date-range-picker__header">
            <button type="button" class="el-picker-panel__icon-btn el-icon-arrow-right"></button>
            <button type="button" class="el-picker-panel__icon-btn el-icon-d-arrow-right"></button>
          </div>
        </div>
      </div>
    `
    const panel = document.querySelector('.el-date-range-picker')
    panel.getBoundingClientRect = () => ({ width: 600, height: 320, top: 0, left: 0, right: 600, bottom: 320 })
    panel.querySelectorAll('button').forEach((btn) => {
      btn.getBoundingClientRect = () => ({ width: 24, height: 24, top: 10, left: 10, right: 34, bottom: 34 })
      Object.defineProperty(btn, 'offsetParent', { value: document.body })
    })

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Meta', metaKey: true, bubbles: true }))
    installDatePickerPanelHints()
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Meta', metaKey: true, bubbles: true }))
    showDatePickerPanelHints()

    const overlay = document.getElementById('cat2bug-date-panel-kbd-overlay')
    expect(overlay).toBeTruthy()
    expect(overlay.querySelectorAll('.cat2bug-date-panel-kbd-hint').length).toBe(4)
  })
})
