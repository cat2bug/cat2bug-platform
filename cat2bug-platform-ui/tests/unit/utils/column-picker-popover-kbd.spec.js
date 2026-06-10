import { COLUMN_PICKER_ITEM_FOCUS_CLASS, COLUMN_PICKER_POPPER_CLASS } from '@/utils/column-picker-popover-kbd'

describe('column-picker-popover-kbd', () => {
  it('exports shared popper and focus class names', () => {
    expect(COLUMN_PICKER_POPPER_CLASS).toBe('defect-column-picker-popover')
    expect(COLUMN_PICKER_ITEM_FOCUS_CLASS).toBe('defect-column-picker-item-focused')
  })
})
