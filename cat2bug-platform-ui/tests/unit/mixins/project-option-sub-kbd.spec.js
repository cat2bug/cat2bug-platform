import projectOptionSubKbd from '@/mixins/project-option-sub-kbd'
import { hasBlockingUiLayer } from '@/plugins/shortcut/service'

jest.mock('@/plugins/shortcut/service', () => ({
  hasBlockingUiLayer: jest.fn(() => false)
}))

describe('project-option-sub-kbd', () => {
  it('registers back action and handles Esc when no blocking layer', () => {
    const goBack = jest.fn()
    const registerPage = jest.fn()
    const vm = {
      _inactive: false,
      $shortcut: { registerPage, unregisterPage: jest.fn() },
      goBack,
      $_projectOptionSubEscBound: false,
      useProjectOptionSubEscShortcut: projectOptionSubKbd.methods.useProjectOptionSubEscShortcut,
      registerProjectOptionSubShortcuts: projectOptionSubKbd.methods.registerProjectOptionSubShortcuts,
      $_attachProjectOptionSubEscListener: projectOptionSubKbd.methods.$_attachProjectOptionSubEscListener
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
      useProjectOptionSubEscShortcut: projectOptionSubKbd.methods.useProjectOptionSubEscShortcut,
      $_attachProjectOptionSubEscListener: projectOptionSubKbd.methods.$_attachProjectOptionSubEscListener,
      $_projectOptionSubEscBound: false
    }
    vm.$_attachProjectOptionSubEscListener.call(vm)
    const event = { key: 'Escape', preventDefault: jest.fn(), stopPropagation: jest.fn() }
    vm.$_onProjectOptionSubEsc(event)
    expect(goBack).not.toHaveBeenCalled()
  })
})
