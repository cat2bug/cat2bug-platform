/**
 * 注册键盘 mixin 桥接的全局运行时。
 *
 * 自动收集 `src/mixins/*.js` 中所有「仅提供 setup()」的桥接 mixin（Vue3 会忽略 mixin 的
 * setup，需要在组件自身生命周期内主动执行），构成识别集合，交由全局 mixin 在组件 created()
 * 阶段按需执行并暴露绑定。
 */
import { createKeyboardBridgeMixin } from '@/composables/keyboard/keyboardBridgeRuntime'

const mixinModules = import.meta.glob('../../mixins/*.js', { eager: true })

const KEYBOARD_BRIDGES = new Set()
Object.values(mixinModules).forEach((mod) => {
  const def = mod && mod.default
  if (def && typeof def === 'object' && typeof def.setup === 'function') {
    KEYBOARD_BRIDGES.add(def)
  }
})

export function installKeyboardMixinBridges(app) {
  app.mixin(createKeyboardBridgeMixin(KEYBOARD_BRIDGES))
}

export { KEYBOARD_BRIDGES }
