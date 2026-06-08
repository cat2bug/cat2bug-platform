/** @jest-environment jsdom */

import {
  REPORT_TEMPLATE_CARD_FOCUS_CLASS,
  clearReportTemplateCardFocus,
  getReportTemplateCards,
  initReportTemplatePopoverKbd,
  isReportTemplatePopoverKbdSuspended,
  isReportTemplatePopoverVisible,
  syncReportTemplateCardFocus
} from '@/utils/report-template-popover-kbd'

describe('report-template-popover-kbd', () => {
  it('getReportTemplateCards returns visible template cards', () => {
    document.body.innerHTML = `
      <div class="report-template-select-popper">
        <div class="template-list">
          <div class="template-list-item"></div>
          <div class="template-list-item"></div>
        </div>
      </div>`
    const popper = document.querySelector('.report-template-select-popper')
    popper.querySelectorAll('.template-list-item').forEach((el) => {
      el.getBoundingClientRect = () => ({ width: 180, height: 140, top: 0, left: 0, right: 180, bottom: 140 })
    })
    expect(getReportTemplateCards(popper)).toHaveLength(2)
  })

  it('syncReportTemplateCardFocus marks focused card', () => {
    document.body.innerHTML = `
      <div id="popper" class="report-template-select-popper">
        <div class="template-list">
          <div class="template-list-item">A</div>
          <div class="template-list-item">B</div>
        </div>
      </div>`
    const popper = document.getElementById('popper')
    popper.querySelectorAll('.template-list-item').forEach((el, i) => {
      el.getBoundingClientRect = () => ({
        width: 180,
        height: 140,
        top: i > 0 ? 150 : 0,
        left: 0,
        right: 180,
        bottom: i > 0 ? 290 : 140
      })
      el.focus = jest.fn()
    })
    const vm = {
      $refs: {
        templatePopover: { popperElm: popper, showPopper: true }
      },
      $nextTick(fn) { if (fn) fn() }
    }
    initReportTemplatePopoverKbd(vm)
    syncReportTemplateCardFocus(vm, 1)
    const cards = popper.querySelectorAll('.template-list-item')
    expect(cards[1].classList.contains(REPORT_TEMPLATE_CARD_FOCUS_CLASS)).toBe(true)
    expect(cards[0].classList.contains(REPORT_TEMPLATE_CARD_FOCUS_CLASS)).toBe(false)
    clearReportTemplateCardFocus(vm)
    expect(cards[1].classList.contains(REPORT_TEMPLATE_CARD_FOCUS_CLASS)).toBe(false)
  })

  it('isReportTemplatePopoverVisible reads showPopper', () => {
    const vm = { $refs: { templatePopover: { showPopper: true } } }
    expect(isReportTemplatePopoverVisible(vm)).toBe(true)
  })

  it('⌘/Ctrl+A triggers add template when popover visible', () => {
    document.body.innerHTML = `
      <div class="report-template-select-popper">
        <a class="report-template-add-link">Add</a>
      </div>`
    const popper = document.querySelector('.report-template-select-popper')
    popper.querySelector('.report-template-add-link').getBoundingClientRect = () => ({
      width: 80, height: 20, top: 0, left: 0, right: 80, bottom: 20
    })
    const addReportTemplate = jest.fn()
    const vm = {
      addReportTemplate,
      $refs: { templatePopover: { popperElm: popper, showPopper: true } },
      $nextTick(fn) { if (fn) fn() }
    }
    initReportTemplatePopoverKbd(vm)
    document.dispatchEvent(new KeyboardEvent('keydown', {
      key: 'a',
      ctrlKey: true,
      bubbles: true,
      cancelable: true
    }))
    expect(addReportTemplate).toHaveBeenCalled()
  })

  it('bare A does not trigger add template', () => {
    document.body.innerHTML = `
      <div class="report-template-select-popper">
        <a class="report-template-add-link">Add</a>
      </div>`
    const popper = document.querySelector('.report-template-select-popper')
    const addReportTemplate = jest.fn()
    const vm = {
      addReportTemplate,
      $refs: { templatePopover: { popperElm: popper, showPopper: true } },
      $nextTick(fn) { if (fn) fn() }
    }
    initReportTemplatePopoverKbd(vm)
    document.dispatchEvent(new KeyboardEvent('keydown', {
      key: 'a',
      bubbles: true,
      cancelable: true
    }))
    expect(addReportTemplate).not.toHaveBeenCalled()
  })

  function cardPopoverVm(overrides = {}) {
    document.body.innerHTML = `
      <div class="report-template-select-popper">
        <div class="template-list">
          <div class="template-list-item">
            <div class="menu">
              <button class="report-template-card-copy"></button>
              <button class="report-template-card-edit"></button>
              <button class="report-template-card-delete"></button>
            </div>
          </div>
        </div>
      </div>`
    const popper = document.querySelector('.report-template-select-popper')
    popper.querySelector('.template-list-item').getBoundingClientRect = () => ({
      width: 180, height: 140, top: 0, left: 0, right: 180, bottom: 140
    })
    popper.querySelectorAll('button').forEach((el) => {
      el.getBoundingClientRect = () => ({ width: 24, height: 24, top: 5, left: 150, right: 174, bottom: 29 })
    })
    const vm = {
      templateList: [{ templateId: 1, templateTitle: 'T1' }],
      copyReportTemplateAtIndex: jest.fn(),
      editReportTemplateAtIndex: jest.fn(),
      deleteReportTemplateAtIndex: jest.fn(),
      $refs: { templatePopover: { popperElm: popper, showPopper: true } },
      $nextTick(fn) { if (fn) fn() },
      ...overrides
    }
    initReportTemplatePopoverKbd(vm)
    syncReportTemplateCardFocus(vm, 0)
    return vm
  }

  it('⌘/Ctrl+V copies focused card', () => {
    const vm = cardPopoverVm()
    document.dispatchEvent(new KeyboardEvent('keydown', {
      key: 'v',
      metaKey: true,
      bubbles: true,
      cancelable: true
    }))
    expect(vm.copyReportTemplateAtIndex).toHaveBeenCalledWith(0)
  })

  it('⌘/Ctrl+E edits focused card', () => {
    const vm = cardPopoverVm()
    document.dispatchEvent(new KeyboardEvent('keydown', {
      key: 'e',
      metaKey: true,
      bubbles: true,
      cancelable: true
    }))
    expect(vm.editReportTemplateAtIndex).toHaveBeenCalledWith(0)
  })

  it('⌘/Ctrl+D deletes focused card', () => {
    const vm = cardPopoverVm()
    document.dispatchEvent(new KeyboardEvent('keydown', {
      key: 'd',
      metaKey: true,
      bubbles: true,
      cancelable: true
    }))
    expect(vm.deleteReportTemplateAtIndex).toHaveBeenCalledWith(0)
  })

  it('Enter does not create report while delete confirm MessageBox is open', () => {
    document.body.innerHTML += `
      <div class="el-message-box__wrapper" style="position:fixed;inset:0;width:100px;height:100px;">
        <div class="el-message-box" style="width:420px;height:180px;"></div>
      </div>`
    const vm = cardPopoverVm({
      createReportFromCardIndex: jest.fn()
    })
    document.dispatchEvent(new KeyboardEvent('keydown', {
      key: 'Enter',
      bubbles: true,
      cancelable: true
    }))
    expect(vm.createReportFromCardIndex).not.toHaveBeenCalled()
    expect(isReportTemplatePopoverKbdSuspended()).toBe(true)
  })
})
