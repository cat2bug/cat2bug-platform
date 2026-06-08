/**
 * 报告「生成报告」模板选择 popover 键盘：
 * - 浮层展开后默认聚焦首张模板卡片
 * - 网格内 ↑↓←→ 移动焦点；Space / Enter 选择；首行 ↑ 或 Esc 关闭
 * - 按住 ⌘/Ctrl：A 添加模版 · B/P 模板分页 · 聚焦卡片上 V 复制 · E 编辑 · D 删除（徽标提示）
 */

import {
  isOnFirstRow,
  moveStatisticGridIndex
} from '@/utils/statistic-grid-kbd'

export const REPORT_TEMPLATE_HOST_CLASS = 'report-template-select-kbd'
export const REPORT_TEMPLATE_CARD_FOCUS_CLASS = 'report-template-card-kbd-current'
export const REPORT_TEMPLATE_POPPER_CLASS = 'report-template-select-popper'

export const REPORT_TEMPLATE_POPOVER_ACTION_KEYS = {
  addTemplate: 'A',
  prevPage: 'B',
  nextPage: 'P'
}

export const REPORT_TEMPLATE_CARD_ACTION_KEYS = {
  copy: 'V',
  edit: 'E',
  delete: 'D'
}

const HINT_OVERLAY_ID = 'cat2bug-report-template-popover-hints'

const OVER_POPOVER_BLOCKING_SELECTORS = [
  '.el-message-box__wrapper',
  '.el-dialog__wrapper',
  '.el-drawer__wrapper'
]

const bindings = new WeakMap()
let popoverModifierHeld = false

function isVisibleLayer(el) {
  if (!el || !el.getBoundingClientRect) return false
  if (el.getAttribute('aria-hidden') === 'true') return false
  const style = window.getComputedStyle(el)
  if (style.display === 'none' || style.visibility === 'hidden') return false
  const rect = el.getBoundingClientRect()
  return rect.width > 0 && rect.height > 0
}

/** MessageBox / Dialog 等叠在浮层上时，勿劫持 Enter 等按键 */
export function isReportTemplatePopoverKbdSuspended() {
  return OVER_POPOVER_BLOCKING_SELECTORS.some((sel) =>
    Array.from(document.querySelectorAll(sel)).some((el) => isVisibleLayer(el))
  )
}

function stopKeyEvent(e) {
  e.preventDefault()
  e.stopPropagation()
  if (typeof e.stopImmediatePropagation === 'function') {
    e.stopImmediatePropagation()
  }
}

function isModifierKeyEvent(e) {
  if (!e) return false
  const k = e.key
  return k === 'Control' || k === 'Meta' || k === 'OS' ||
    e.keyCode === 17 || e.keyCode === 91 || e.keyCode === 93
}

function isOpenKey(key) {
  return key === 'ArrowDown' || key === 'Down' ||
    key === 'Enter' || key === ' ' || key === 'Spacebar'
}

function isActivateKey(key) {
  return key === 'Enter' || key === ' ' || key === 'Spacebar'
}

function directionFromKey(key) {
  if (key === 'ArrowLeft' || key === 'Left') return 'left'
  if (key === 'ArrowRight' || key === 'Right') return 'right'
  if (key === 'ArrowUp' || key === 'Up') return 'up'
  if (key === 'ArrowDown' || key === 'Down') return 'down'
  return null
}

export function getReportTemplatePopoverVm(vm) {
  return vm && vm.$refs && vm.$refs.templatePopover
}

export function getReportTemplatePopperEl(vm) {
  const pop = getReportTemplatePopoverVm(vm)
  if (!pop) return null
  return pop.popperElm || (pop.$refs && pop.$refs.popper) || null
}

export function isReportTemplatePopoverVisible(vm) {
  const pop = getReportTemplatePopoverVm(vm)
  return !!(pop && pop.showPopper)
}

export function getReportTemplateCards(popperEl) {
  if (!popperEl || !popperEl.querySelectorAll) return []
  return Array.from(popperEl.querySelectorAll('.template-list-item')).filter((el) => {
    const rect = el.getBoundingClientRect()
    return rect.width > 0 && rect.height > 0
  })
}

export function getReportTemplatePaginationEl(vm) {
  const popper = getReportTemplatePopperEl(vm)
  if (!popper) return null
  return popper.querySelector('.report-template-pagination')
}

export function isReportTemplateTriggerFocused(vm) {
  const btn = vm.$refs.triggerButton
  const el = btn && (btn.$el || btn)
  return !!(el && document.activeElement === el)
}

export function focusReportTemplateTrigger(vm) {
  const btn = vm.$refs.triggerButton
  const el = btn && (btn.$el || btn)
  if (!el || typeof el.focus !== 'function') return
  try {
    el.focus({ preventScroll: false })
  } catch (e) {
    el.focus()
  }
}

export function clearReportTemplateCardFocus(vm) {
  clearReportTemplatePopoverFocus(vm)
}

export function clearReportTemplatePopoverFocus(vm) {
  const popper = getReportTemplatePopperEl(vm)
  if (!popper) return
  popper.querySelectorAll(`.${REPORT_TEMPLATE_CARD_FOCUS_CLASS}`).forEach((el) => {
    el.classList.remove(REPORT_TEMPLATE_CARD_FOCUS_CLASS)
  })
}

export function syncReportTemplateCardFocus(vm, index) {
  clearReportTemplatePopoverFocus(vm)
  const cards = getReportTemplateCards(getReportTemplatePopperEl(vm))
  const card = cards[index]
  if (!card) return false
  card.classList.add(REPORT_TEMPLATE_CARD_FOCUS_CLASS)
  if (!card.hasAttribute('tabindex')) {
    card.setAttribute('tabindex', '-1')
  }
  try {
    card.focus({ preventScroll: false })
  } catch (e) {
    card.focus()
  }
  const rec = bindings.get(vm)
  if (rec) {
    rec.state.cardIndex = index
    rec.state.inGrid = true
  }
  if (popoverModifierHeld) {
    syncPopoverModifierHints(vm)
  }
  return true
}

/** 浮层 DOM 就绪后聚焦指定卡片（默认首张） */
export function focusReportTemplateCardWhenReady(vm, cardIndex = 0, attempt = 0) {
  if (!isReportTemplatePopoverVisible(vm)) return
  vm.$nextTick(() => {
    vm.$nextTick(() => {
      if (!isReportTemplatePopoverVisible(vm)) return
      const cards = getReportTemplateCards(getReportTemplatePopperEl(vm))
      if (cards.length) {
        syncReportTemplateCardFocus(vm, Math.min(cardIndex, cards.length - 1))
        return
      }
      if (attempt < 8) {
        setTimeout(() => focusReportTemplateCardWhenReady(vm, cardIndex, attempt + 1), 40)
      }
    })
  })
}

function getFocusedCardIndex(vm) {
  const rec = bindings.get(vm)
  if (!rec || !rec.state.inGrid || rec.state.cardIndex < 0) return -1
  return rec.state.cardIndex
}

function noopEvent() {
  return { stopPropagation() {} }
}

function activateCardCopy(vm) {
  const idx = getFocusedCardIndex(vm)
  if (idx < 0) return false
  if (typeof vm.copyReportTemplateAtIndex === 'function') {
    vm.copyReportTemplateAtIndex(idx)
    return true
  }
  const template = vm.templateList && vm.templateList[idx]
  if (!template || typeof vm.copyReportTemplate !== 'function') return false
  vm.copyReportTemplate(noopEvent(), template)
  return true
}

function activateCardEdit(vm) {
  const idx = getFocusedCardIndex(vm)
  if (idx < 0) return false
  if (typeof vm.editReportTemplateAtIndex === 'function') {
    vm.editReportTemplateAtIndex(idx)
    return true
  }
  const template = vm.templateList && vm.templateList[idx]
  if (!template || typeof vm.goReportTemplateView !== 'function') return false
  vm.goReportTemplateView(noopEvent(), template.templateId)
  return true
}

function activateCardDelete(vm) {
  const idx = getFocusedCardIndex(vm)
  if (idx < 0) return false
  if (typeof vm.deleteReportTemplateAtIndex === 'function') {
    vm.deleteReportTemplateAtIndex(idx)
    return true
  }
  const template = vm.templateList && vm.templateList[idx]
  if (!template || typeof vm.deleteReport !== 'function') return false
  vm.deleteReport(noopEvent(), template.templateId)
  return true
}

function getFocusedCardMenuAnchors(vm) {
  const popper = getReportTemplatePopperEl(vm)
  const cards = getReportTemplateCards(popper)
  const idx = getFocusedCardIndex(vm)
  if (idx < 0 || !cards[idx]) return []
  const menu = cards[idx].querySelector('.menu')
  if (!menu) return []
  const specs = [
    { el: menu.querySelector('.report-template-card-copy'), letter: REPORT_TEMPLATE_CARD_ACTION_KEYS.copy },
    { el: menu.querySelector('.report-template-card-edit'), letter: REPORT_TEMPLATE_CARD_ACTION_KEYS.edit },
    { el: menu.querySelector('.report-template-card-delete'), letter: REPORT_TEMPLATE_CARD_ACTION_KEYS.delete }
  ]
  return specs.filter((spec) => {
    if (!spec.el) return false
    const rect = spec.el.getBoundingClientRect()
    return rect.width > 0 && rect.height > 0
  })
}

function activateAddTemplate(vm) {
  if (typeof vm.addReportTemplate === 'function') {
    vm.addReportTemplate()
    return true
  }
  const popper = getReportTemplatePopperEl(vm)
  const link = popper && popper.querySelector('.report-template-add-link')
  if (link && typeof link.click === 'function') {
    link.click()
    return true
  }
  return false
}

function clickTemplatePagination(vm, delta) {
  const pag = getReportTemplatePaginationEl(vm)
  if (!pag) return false
  const sel = delta < 0 ? '.btn-prev' : '.btn-next'
  const btn = pag.querySelector(sel)
  if (!btn || btn.classList.contains('disabled') || typeof btn.click !== 'function') {
    return false
  }
  btn.click()
  return true
}

function handlePopoverModifierShortcut(e, vm) {
  if (!isReportTemplatePopoverVisible(vm)) return false
  if (isReportTemplatePopoverKbdSuspended()) return false
  if (!(e.metaKey || e.ctrlKey) || e.altKey || e.shiftKey || e.isComposing) return false
  if (!e.key || e.key.length !== 1) return false
  const letter = e.key.toUpperCase()
  if (letter === REPORT_TEMPLATE_POPOVER_ACTION_KEYS.addTemplate) {
    stopKeyEvent(e)
    activateAddTemplate(vm)
    return true
  }
  if (letter === REPORT_TEMPLATE_POPOVER_ACTION_KEYS.prevPage) {
    stopKeyEvent(e)
    clickTemplatePagination(vm, -1)
    return true
  }
  if (letter === REPORT_TEMPLATE_POPOVER_ACTION_KEYS.nextPage) {
    stopKeyEvent(e)
    clickTemplatePagination(vm, 1)
    return true
  }
  if (letter === REPORT_TEMPLATE_CARD_ACTION_KEYS.copy) {
    stopKeyEvent(e)
    return activateCardCopy(vm)
  }
  if (letter === REPORT_TEMPLATE_CARD_ACTION_KEYS.edit) {
    stopKeyEvent(e)
    return activateCardEdit(vm)
  }
  if (letter === REPORT_TEMPLATE_CARD_ACTION_KEYS.delete) {
    stopKeyEvent(e)
    return activateCardDelete(vm)
  }
  return false
}

function ensureHintOverlay() {
  let el = document.getElementById(HINT_OVERLAY_ID)
  if (!el) {
    el = document.createElement('div')
    el.id = HINT_OVERLAY_ID
    el.className = 'cat2bug-page-kbd-hint-layer report-template-popover-hint-layer'
    el.setAttribute('aria-hidden', 'true')
    document.body.appendChild(el)
  }
  return el
}

function hideReportTemplatePopoverHints() {
  const overlay = document.getElementById(HINT_OVERLAY_ID)
  if (overlay) overlay.innerHTML = ''
}

function getHintAnchors(vm) {
  const popper = getReportTemplatePopperEl(vm)
  if (!popper) return []
  const specs = [
    { el: popper.querySelector('.report-template-add-link'), letter: REPORT_TEMPLATE_POPOVER_ACTION_KEYS.addTemplate },
    { el: popper.querySelector('.report-template-pagination .btn-prev'), letter: REPORT_TEMPLATE_POPOVER_ACTION_KEYS.prevPage, skipDisabled: true },
    { el: popper.querySelector('.report-template-pagination .btn-next'), letter: REPORT_TEMPLATE_POPOVER_ACTION_KEYS.nextPage, skipDisabled: true }
  ]
  return specs.filter((spec) => {
    if (!spec.el) return false
    if (spec.skipDisabled && spec.el.classList.contains('disabled')) return false
    const rect = spec.el.getBoundingClientRect()
    return rect.width > 0 && rect.height > 0
  }).concat(getFocusedCardMenuAnchors(vm))
}

function renderReportTemplatePopoverHints(vm) {
  if (!popoverModifierHeld || !isReportTemplatePopoverVisible(vm)) {
    hideReportTemplatePopoverHints()
    return
  }
  const overlay = ensureHintOverlay()
  overlay.innerHTML = ''
  getHintAnchors(vm).forEach(({ el, letter }) => {
    const rect = el.getBoundingClientRect()
    const badge = document.createElement('span')
    badge.className = 'cat2bug-page-kbd-hint-float defect-list-kbd-hint report-template-popover-hint-float'
    badge.textContent = letter
    badge.setAttribute('aria-hidden', 'true')
    badge.style.cssText = [
      'position:fixed',
      `left:${Math.round(rect.right + 2)}px`,
      `top:${Math.round(rect.bottom + 2)}px`,
      'transform:translate(-100%,-100%)',
      'z-index:100002',
      'pointer-events:none'
    ].join(';')
    overlay.appendChild(badge)
  })
}

function syncPopoverModifierHints(vm) {
  if (popoverModifierHeld && isReportTemplatePopoverVisible(vm)) {
    renderReportTemplatePopoverHints(vm)
  } else {
    hideReportTemplatePopoverHints()
  }
}

function handlePopoverModifierEvent(e, vm) {
  if (!isReportTemplatePopoverVisible(vm)) {
    if (isModifierKeyEvent(e) && e.type === 'keyup') {
      popoverModifierHeld = false
      hideReportTemplatePopoverHints()
    }
    return
  }
  if (isReportTemplatePopoverKbdSuspended()) {
    if (e.type === 'keyup' && isModifierKeyEvent(e)) {
      popoverModifierHeld = false
      hideReportTemplatePopoverHints()
    }
    return
  }
  if (e.type === 'keydown') {
    if (isModifierKeyEvent(e) && !e.shiftKey) {
      popoverModifierHeld = true
      syncPopoverModifierHints(vm)
      return
    }
    if ((e.metaKey || e.ctrlKey) && !e.altKey && !e.shiftKey) {
      popoverModifierHeld = true
      syncPopoverModifierHints(vm)
    }
  }
  if (e.type === 'keyup' && isModifierKeyEvent(e)) {
    popoverModifierHeld = false
    hideReportTemplatePopoverHints()
  }
}

export function openReportTemplatePopover(vm, options = {}) {
  const pop = getReportTemplatePopoverVm(vm)
  if (pop) {
    if (typeof pop.doShow === 'function') {
      pop.doShow()
    } else {
      pop.showPopper = true
    }
  }
  if (options.focusTrigger) {
    vm.$nextTick(() => focusReportTemplateTrigger(vm))
    return
  }
  const cardIndex = options.focusCardIndex != null ? options.focusCardIndex : 0
  focusReportTemplateCardWhenReady(vm, cardIndex)
}

export function closeReportTemplatePopover(vm) {
  const pop = getReportTemplatePopoverVm(vm)
  if (pop) {
    if (typeof pop.doClose === 'function') {
      pop.doClose()
    } else {
      pop.showPopper = false
    }
  }
  clearReportTemplatePopoverFocus(vm)
  popoverModifierHeld = false
  hideReportTemplatePopoverHints()
  const rec = bindings.get(vm)
  if (rec) {
    rec.state.cardIndex = -1
    rec.state.inGrid = false
  }
}

function openPopoverAndEnterGrid(vm, cardIndex = 0) {
  const pop = getReportTemplatePopoverVm(vm)
  if (!pop) return
  if (!pop.showPopper) {
    if (typeof pop.doShow === 'function') {
      pop.doShow()
    } else {
      pop.showPopper = true
    }
  }
  focusReportTemplateCardWhenReady(vm, cardIndex)
}

function activateFocusedCard(vm) {
  const rec = bindings.get(vm)
  const cards = getReportTemplateCards(getReportTemplatePopperEl(vm))
  if (!cards.length || !rec || !rec.state.inGrid || rec.state.cardIndex < 0) return
  const idx = rec.state.cardIndex
  if (typeof vm.createReportFromCardIndex === 'function') {
    vm.createReportFromCardIndex(idx)
  } else {
    const card = cards[idx]
    if (card && typeof card.click === 'function') card.click()
  }
}

function handleDocumentKeydown(e, vm) {
  const pop = getReportTemplatePopoverVm(vm)
  if (!pop) return

  handlePopoverModifierEvent(e, vm)

  const rec = bindings.get(vm)
  const state = rec ? rec.state : { cardIndex: -1, inGrid: false }
  const key = e.key
  const visible = isReportTemplatePopoverVisible(vm)
  const triggerFocused = isReportTemplateTriggerFocused(vm)

  if (!visible) {
    if (!triggerFocused || !isOpenKey(key)) return
    stopKeyEvent(e)
    openPopoverAndEnterGrid(vm, 0)
    return
  }

  if (isReportTemplatePopoverKbdSuspended()) return

  if (handlePopoverModifierShortcut(e, vm)) return

  const cards = getReportTemplateCards(getReportTemplatePopperEl(vm))
  const dir = directionFromKey(key)
  const isNav = dir || isActivateKey(key) || key === 'Escape' || key === 'Esc'
  if (!isNav) return
  if (!cards.length && key !== 'Escape' && key !== 'Esc') return

  stopKeyEvent(e)

  if (key === 'Escape' || key === 'Esc') {
    closeReportTemplatePopover(vm)
    focusReportTemplateTrigger(vm)
    return
  }

  let idx = state.inGrid ? state.cardIndex : -1

  if (dir === 'down') {
    if (idx < 0) {
      syncReportTemplateCardFocus(vm, 0)
      return
    }
    const next = moveStatisticGridIndex(cards, idx, 'down')
    if (next !== idx) syncReportTemplateCardFocus(vm, next)
    return
  }

  if (dir === 'up') {
    if (idx < 0) {
      syncReportTemplateCardFocus(vm, 0)
      return
    }
    if (isOnFirstRow(cards, idx)) {
      closeReportTemplatePopover(vm)
      focusReportTemplateTrigger(vm)
      return
    }
    const next = moveStatisticGridIndex(cards, idx, 'up')
    syncReportTemplateCardFocus(vm, next)
    return
  }

  if (dir === 'left' || dir === 'right') {
    if (idx < 0) {
      syncReportTemplateCardFocus(vm, 0)
      return
    }
    const next = moveStatisticGridIndex(cards, idx, dir)
    syncReportTemplateCardFocus(vm, next)
    return
  }

  if (isActivateKey(key)) {
    if (idx < 0) {
      syncReportTemplateCardFocus(vm, 0)
      return
    }
    activateFocusedCard(vm)
  }
}

function handleDocumentKeyup(e, vm) {
  handlePopoverModifierEvent(e, vm)
}

export function initReportTemplatePopoverKbd(vm) {
  if (!vm || bindings.has(vm)) return
  const state = { cardIndex: -1, inGrid: false }
  const handler = (e) => handleDocumentKeydown(e, vm)
  const keyupHandler = (e) => handleDocumentKeyup(e, vm)
  bindings.set(vm, { state, handler, keyupHandler })

  document.addEventListener('keydown', handler, true)
  document.addEventListener('keyup', keyupHandler, true)

  vm.$nextTick(() => {
    const pop = getReportTemplatePopoverVm(vm)
    if (!pop || pop._cat2bugReportTplKbdWatch) return
    pop._cat2bugReportTplKbdWatch = pop.$watch('showPopper', (visible) => {
      if (visible) {
        focusReportTemplateCardWhenReady(vm, 0)
        return
      }
      clearReportTemplatePopoverFocus(vm)
      popoverModifierHeld = false
      hideReportTemplatePopoverHints()
      state.cardIndex = -1
      state.inGrid = false
    })
  })
}

export function destroyReportTemplatePopoverKbd(vm) {
  const rec = bindings.get(vm)
  if (!rec) return
  document.removeEventListener('keydown', rec.handler, true)
  document.removeEventListener('keyup', rec.keyupHandler, true)
  bindings.delete(vm)
  popoverModifierHeld = false
  hideReportTemplatePopoverHints()
  const pop = getReportTemplatePopoverVm(vm)
  if (pop && pop._cat2bugReportTplKbdWatch) {
    pop._cat2bugReportTplKbdWatch()
    pop._cat2bugReportTplKbdWatch = null
  }
}

/** 页面快捷键 E：展开浮层并聚焦首张模板卡片 */
export function shortcutOpenReportTemplatePopover(vm) {
  openReportTemplatePopover(vm)
}
