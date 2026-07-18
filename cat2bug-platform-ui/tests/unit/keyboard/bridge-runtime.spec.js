import { createSSRApp, h, ref } from 'vue'
import { renderToString } from '@vue/server-renderer'
import {
  createKeyboardBridgeMixin,
  invokeBridgeSetup
} from '@/composables/keyboard/keyboardBridgeRuntime'
import { useLegacyKeyboardMixin } from '@/composables/keyboard/useLegacyKeyboardMixin'

function captureVueWarnings(fn) {
  const warnings = []
  const spy = jest.spyOn(console, 'warn').mockImplementation((msg) => warnings.push(String(msg)))
  return Promise.resolve(fn()).then((value) => {
    spy.mockRestore()
    return { value, warnings }
  }, (err) => {
    spy.mockRestore()
    throw err
  })
}

describe('keyboardBridgeRuntime', () => {
  test('exposes bridge setup() bindings (ref + method) on the instance during render', async () => {
    const bridge = {
      setup() {
        const active = ref(true)
        return { navActive: active, onShow: () => 'shown' }
      }
    }
    const bridgeSet = new Set([bridge])

    const Comp = {
      mixins: [bridge],
      render() {
        // 访问桥接绑定：若未暴露会触发 Vue 告警并渲染 undefined
        return h('div', `active=${this.navActive} handler=${typeof this.onShow}`)
      }
    }

    const app = createSSRApp(Comp)
    app.mixin(createKeyboardBridgeMixin(bridgeSet))

    const { value: html, warnings } = await captureVueWarnings(() => renderToString(app))
    expect(html).toContain('active=true')
    expect(html).toContain('handler=function')
    expect(warnings.join('\n')).not.toMatch(/was accessed during render but is not defined/)
  })

  test('component own methods/computed win over bridge bindings of the same name', async () => {
    const bridge = {
      setup() {
        return { label: 'from-bridge', doClose: () => 'bridge-close' }
      }
    }
    const bridgeSet = new Set([bridge])

    const Comp = {
      mixins: [bridge],
      computed: {
        label() { return 'from-component' }
      },
      methods: {
        doClose() { return 'component-close' }
      },
      render() {
        return h('div', `${this.label}|${this.doClose()}`)
      }
    }

    const app = createSSRApp(Comp)
    app.mixin(createKeyboardBridgeMixin(bridgeSet))

    const { value: html } = await captureVueWarnings(() => renderToString(app))
    expect(html).toContain('from-component')
    expect(html).toContain('component-close')
    expect(html).not.toContain('from-bridge')
  })

  test('invokeBridgeSetup runs a bridge only once per instance', async () => {
    let calls = 0
    const bridge = {
      setup() {
        calls += 1
        return { tick: calls }
      }
    }
    const bridgeSet = new Set([bridge])

    const Comp = {
      mixins: [bridge],
      created() {
        // 组件自身再次触发同一桥接，应命中缓存、不再执行 setup
        invokeBridgeSetup(bridge)
        invokeBridgeSetup(bridge)
      },
      render() { return h('div', `tick=${this.tick}`) }
    }

    const app = createSSRApp(Comp)
    app.mixin(createKeyboardBridgeMixin(bridgeSet))

    await captureVueWarnings(() => renderToString(app))
    expect(calls).toBe(1)
  })

  test('useLegacyKeyboardMixin runs nested setup-only mixins and honors component overrides', async () => {
    const nestedBridge = {
      setup() {
        const flag = ref(true)
        return { nestedFlag: flag, nestedFn: () => 'nested' }
      }
    }
    const legacyContainer = {
      mixins: [nestedBridge],
      data() {
        return { containerState: 'state' }
      },
      methods: {
        containerMethod() { return 'container' },
        // 该默认实现应被组件覆盖
        overridable() { return this.$_defaultOverridable() },
        $_defaultOverridable() { return 'default' }
      }
    }
    const containerBridge = {
      setup() {
        return useLegacyKeyboardMixin(legacyContainer)
      }
    }
    const bridgeSet = new Set([containerBridge])

    const Comp = {
      mixins: [containerBridge],
      methods: {
        overridable() { return 'overridden' }
      },
      render() {
        return h('div', [
          `nested=${this.nestedFlag}`,
          `nestedFn=${this.nestedFn()}`,
          `container=${this.containerMethod()}`,
          `state=${this.containerState}`,
          `override=${this.overridable()}`
        ].join('|'))
      }
    }

    const app = createSSRApp(Comp)
    app.mixin(createKeyboardBridgeMixin(bridgeSet))

    const { value: html, warnings } = await captureVueWarnings(() => renderToString(app))
    expect(html).toContain('nested=true')
    expect(html).toContain('nestedFn=nested')
    expect(html).toContain('container=container')
    expect(html).toContain('state=state')
    expect(html).toContain('override=overridden')
    expect(warnings.join('\n')).not.toMatch(/was accessed during render but is not defined/)
  })
})
