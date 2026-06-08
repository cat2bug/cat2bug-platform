/** @jest-environment jsdom */

import {
  closeSplitDropdown,
  createDropdownMenuKeyboardState,
  DROPDOWN_HOST_SELECTOR,
  isSplitDropdownOpen,
  shouldSkipDropdownHost,
  shortcutOpenSplitDropdown,
  SPLIT_DROPDOWN_HOST_CLASS
} from '@/utils/split-dropdown-kbd'

jest.mock('@/plugins/shortcut/service', () => ({
  hasBlockingUiLayer: jest.fn(() => false)
}))

jest.mock('@/utils/defect-drawer-shortcuts', () => ({
  findTopFormDrawerVm: jest.fn(() => null)
}))

import { hasBlockingUiLayer } from '@/plugins/shortcut/service'
import { findTopFormDrawerVm } from '@/utils/defect-drawer-shortcuts'

describe('split-dropdown-kbd', () => {
  it('exports global dropdown host selector', () => {
    expect(DROPDOWN_HOST_SELECTOR).toBe('.el-dropdown')
  })

  it('keeps legacy split host class for styling hooks', () => {
    expect(SPLIT_DROPDOWN_HOST_CLASS).toBe('cat2bug-split-dropdown-kbd')
  })

  it('createDropdownMenuKeyboardState starts at first item', () => {
    expect(createDropdownMenuKeyboardState()).toEqual({ menuIndex: 0 })
  })

  it('shouldSkipDropdownHost respects opt-out attribute', () => {
    const host = { hasAttribute: (name) => name === 'data-cat2bug-dropdown-kbd-skip' }
    expect(shouldSkipDropdownHost(host)).toBe(true)
    expect(shouldSkipDropdownHost(null)).toBe(true)
    expect(shouldSkipDropdownHost({ hasAttribute: () => false })).toBe(false)
  })

  it('shortcutOpenSplitDropdown returns false when form drawer is open', async () => {
    const root = document.createElement('div')
    root.innerHTML = `
      <div class="case-add-dropdown cat2bug-split-dropdown-kbd">
        <button type="button">新建用例</button>
      </div>
    `
    findTopFormDrawerVm.mockReturnValueOnce({ visible: true })
    await expect(shortcutOpenSplitDropdown(root, '.case-add-dropdown.cat2bug-split-dropdown-kbd')).resolves.toBe(false)
  })

  it('shortcutOpenSplitDropdown returns false when blocking ui layer exists', async () => {
    const root = document.createElement('div')
    root.innerHTML = `
      <div class="case-add-dropdown cat2bug-split-dropdown-kbd">
        <button type="button">新建用例</button>
      </div>
    `
    hasBlockingUiLayer.mockReturnValueOnce(true)
    await expect(shortcutOpenSplitDropdown(root, '.case-add-dropdown.cat2bug-split-dropdown-kbd')).resolves.toBe(false)
  })

  it('closeSplitDropdown sets visible false immediately even when hide throws', () => {
    const host = document.createElement('div')
    host.className = 'el-dropdown cat2bug-split-dropdown-kbd'
    const dropdownVm = {
      visible: true,
      timeout: setTimeout(() => {}, 1000),
      removeTabindex() {
        throw new Error('triggerElm missing')
      }
    }
    host.__vue__ = dropdownVm
    closeSplitDropdown(host)
    expect(dropdownVm.visible).toBe(false)
    expect(dropdownVm.timeout).toBeNull()
  })
})
