/** @jest-environment jsdom */

import {
  PROJECT_ICON_ITEM_FOCUS_CLASS,
  PROJECT_ICON_POPOVER_HOST_CLASS,
  PROJECT_ICON_POPPER_CLASS,
  clearProjectIconPopoverFocus,
  getProjectIconPopoverItems,
  getProjectIconPopperEl,
  initProjectIconPopoverKbd,
  isAnyProjectIconPopoverOpenInDom,
  syncProjectIconItemFocus
} from '@/utils/project-icon-popover-kbd'

function mockRect(left, top, width = 130, height = 130) {
  return { left, top, width, height, right: left + width, bottom: top + height }
}

function buildPopperDom() {
  document.body.innerHTML = `
    <div class="el-popover project-icon-popper">
      <div class="project-icon-popper">
        ${[0, 1, 2, 3, 4].map((i) => `<div class="project-icon-item"><div class="el-image"></div></div>`).join('')}
      </div>
    </div>`
  const popper = document.querySelector('.el-popover.project-icon-popper')
  const items = popper.querySelectorAll('.project-icon-item')
  items.forEach((el, i) => {
    const col = i % 4
    const row = Math.floor(i / 4)
    el.getBoundingClientRect = () => mockRect(col * 140, row * 140)
    el.focus = jest.fn()
  })
  return popper
}

describe('project-icon-popover-kbd', () => {
  it('exports stable class names', () => {
    expect(PROJECT_ICON_POPOVER_HOST_CLASS).toBe('project-icon-popover-kbd')
    expect(PROJECT_ICON_POPPER_CLASS).toBe('project-icon-popper')
    expect(PROJECT_ICON_ITEM_FOCUS_CLASS).toBe('project-icon-item-kbd-current')
  })

  it('getProjectIconPopperEl falls back to visible popper in document', () => {
    const popper = buildPopperDom()
    const vm = { $refs: { projectIconPopover: {} }, projectIconPopperVisible: true }
    expect(getProjectIconPopperEl(vm)).toBe(popper)
  })

  it('syncProjectIconItemFocus marks focused item', () => {
    const popper = buildPopperDom()
    const vm = {
      projectIconPopperVisible: true,
      $refs: { projectIconPopover: { popperElm: popper, showPopper: true } },
      $nextTick(fn) { if (fn) fn() },
      $watch: jest.fn()
    }
    initProjectIconPopoverKbd(vm)
    syncProjectIconItemFocus(vm, 2)
    const items = getProjectIconPopoverItems(popper)
    expect(items[2].classList.contains(PROJECT_ICON_ITEM_FOCUS_CLASS)).toBe(true)
    expect(items[0].classList.contains(PROJECT_ICON_ITEM_FOCUS_CLASS)).toBe(false)
    clearProjectIconPopoverFocus(vm)
    expect(items[2].classList.contains(PROJECT_ICON_ITEM_FOCUS_CLASS)).toBe(false)
  })

  it('ArrowRight moves focus to next icon in grid', () => {
    const popper = buildPopperDom()
    const vm = {
      projectIconPopperVisible: true,
      $refs: { projectIconPopover: { popperElm: popper, showPopper: true } },
      $nextTick(fn) { if (fn) fn() },
      $watch: jest.fn()
    }
    initProjectIconPopoverKbd(vm)
    syncProjectIconItemFocus(vm, 0)
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }))
    const items = getProjectIconPopoverItems(popper)
    expect(items[1].classList.contains(PROJECT_ICON_ITEM_FOCUS_CLASS)).toBe(true)
  })

  it('ArrowDown moves focus to next row', () => {
    const popper = buildPopperDom()
    const vm = {
      projectIconPopperVisible: true,
      $refs: { projectIconPopover: { popperElm: popper, showPopper: true } },
      $nextTick(fn) { if (fn) fn() },
      $watch: jest.fn()
    }
    initProjectIconPopoverKbd(vm)
    syncProjectIconItemFocus(vm, 0)
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true }))
    const items = getProjectIconPopoverItems(popper)
    expect(items[4].classList.contains(PROJECT_ICON_ITEM_FOCUS_CLASS)).toBe(true)
  })

  it('isAnyProjectIconPopoverOpenInDom detects visible popper', () => {
    buildPopperDom()
    expect(isAnyProjectIconPopoverOpenInDom()).toBe(true)
  })

  it('Esc closes popover and keeps form page open', () => {
    const popper = buildPopperDom()
    const doClose = jest.fn()
    const vm = {
      projectIconPopperVisible: true,
      $refs: {
        projectIconPopover: { popperElm: popper, showPopper: true, doClose },
        projectIconTrigger: { $el: { focus: jest.fn() } }
      },
      $nextTick(fn) { if (fn) fn() },
      $watch: jest.fn()
    }
    initProjectIconPopoverKbd(vm)
    syncProjectIconItemFocus(vm, 0)
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
    expect(doClose).toHaveBeenCalled()
    expect(vm.projectIconPopperVisible).toBe(false)
  })
})
