/** @jest-environment jsdom */

import { projectOptionSubKbdLegacyMixin } from '@/composables/keyboard/useProjectOptionSubKbd'
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'

jest.mock('@/plugins/shortcut/service', () => ({
  hasBlockingUiLayer: jest.fn(() => false)
}))

const methods = projectOptionSubKbdLegacyMixin.methods

describe('project-option-sub-kbd', () => {
  it('registers back action and handles Esc when no blocking layer', () => {
    const goBack = jest.fn()
    const registerPage = jest.fn()
    const vm = {
      _inactive: false,
      $shortcut: { registerPage, unregisterPage: jest.fn() },
      goBack,
      $_projectOptionSubEscBound: false,
      useProjectOptionSubEscShortcut: methods.useProjectOptionSubEscShortcut,
      registerProjectOptionSubShortcuts: methods.registerProjectOptionSubShortcuts,
      $_attachProjectOptionSubEscListener: methods.$_attachProjectOptionSubEscListener
    }
    vm.registerProjectOptionSubShortcuts.call(vm)
    expect(registerPage).toHaveBeenCalledWith(
      'project-option',
      expect.arrayContaining([
        expect.objectContaining({ key: 'back', defaultLetter: 'B' })
      ])
    )

    vm.$_attachProjectOptionSubEscListener.call(vm)
    const event = { key: 'Escape', preventDefault: jest.fn(), stopPropagation: jest.fn() }
    vm.$_onProjectOptionSubEsc(event)
    expect(goBack).toHaveBeenCalled()
  })

  it('does not go back on Esc when blocking UI layer is open', () => {
    hasBlockingUiLayer.mockReturnValueOnce(true)
    const goBack = jest.fn()
    const vm = {
      _inactive: false,
      goBack,
      useProjectOptionSubEscShortcut: methods.useProjectOptionSubEscShortcut,
      $_attachProjectOptionSubEscListener: methods.$_attachProjectOptionSubEscListener,
      $_projectOptionSubEscBound: false
    }
    vm.$_attachProjectOptionSubEscListener.call(vm)
    const event = { key: 'Escape', preventDefault: jest.fn(), stopPropagation: jest.fn() }
    vm.$_onProjectOptionSubEsc(event)
    expect(goBack).not.toHaveBeenCalled()
  })
})
