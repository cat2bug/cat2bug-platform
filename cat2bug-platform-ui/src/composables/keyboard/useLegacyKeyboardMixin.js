import {
  computed,
  getCurrentInstance,
  onActivated,
  onBeforeMount,
  onBeforeUnmount,
  onDeactivated,
  onMounted,
  onUnmounted,
  onUpdated,
  reactive,
  toRefs,
  watch
} from 'vue'
import { invokeBridgeSetup } from '@/composables/keyboard/keyboardBridgeRuntime'

const LIFECYCLE_HOOKS = [
  ['beforeMount', onBeforeMount],
  ['mounted', onMounted],
  ['updated', onUpdated],
  ['activated', onActivated],
  ['deactivated', onDeactivated],
  ['beforeDestroy', onBeforeUnmount],
  ['beforeUnmount', onBeforeUnmount],
  ['destroyed', onUnmounted],
  ['unmounted', onUnmounted]
]

function flattenMixins(mixin, list = []) {
  if (!mixin || typeof mixin !== 'object') return list
  const inner = Array.isArray(mixin.mixins) ? mixin.mixins : []
  inner.forEach((child) => flattenMixins(child, list))
  list.push(mixin)
  return list
}

function getByPath(source, path) {
  if (!path || typeof path !== 'string') return undefined
  const parts = path.split('.')
  let value = source
  for (let i = 0; i < parts.length; i += 1) {
    if (value == null) return undefined
    value = value[parts[i]]
  }
  return value
}

function createLegacyContext(proxy, state, methods, computedRefs, ownOverrideKeys) {
  const overrides = ownOverrideKeys || new Set()
  return new Proxy({}, {
    get(_, key) {
      // 组件自身在 methods/computed 里定义的成员优先于 mixin 默认实现，
      // 保持 Vue2 时代「组件覆盖 mixin」的合并语义。
      if (overrides.has(key) && key in proxy) return proxy[key]
      if (Object.prototype.hasOwnProperty.call(methods, key)) return methods[key]
      if (Object.prototype.hasOwnProperty.call(computedRefs, key)) return computedRefs[key].value
      if (Object.prototype.hasOwnProperty.call(state, key)) return state[key]
      return proxy[key]
    },
    set(_, key, value) {
      if (Object.prototype.hasOwnProperty.call(methods, key)) {
        methods[key] = value
        return true
      }
      if (Object.prototype.hasOwnProperty.call(computedRefs, key)) {
        computedRefs[key].value = value
        return true
      }
      if (Object.prototype.hasOwnProperty.call(state, key)) {
        state[key] = value
        return true
      }
      proxy[key] = value
      return true
    },
    has(_, key) {
      return Object.prototype.hasOwnProperty.call(methods, key) ||
        Object.prototype.hasOwnProperty.call(computedRefs, key) ||
        Object.prototype.hasOwnProperty.call(state, key) ||
        key in proxy
    }
  })
}

function registerWatch(watchKey, watchEntry, ctx) {
  const entries = Array.isArray(watchEntry) ? watchEntry : [watchEntry]
  entries.forEach((entry) => {
    let handler = entry
    const options = {}

    if (entry && typeof entry === 'object' && !Array.isArray(entry)) {
      handler = entry.handler
      if (entry.deep != null) options.deep = !!entry.deep
      if (entry.immediate != null) options.immediate = !!entry.immediate
    }

    if (typeof handler === 'string') {
      handler = ctx[handler]
    }
    if (typeof handler !== 'function') return

    const source = typeof watchKey === 'function'
      ? () => watchKey.call(ctx)
      : () => getByPath(ctx, watchKey)

    watch(
      source,
      (value, oldValue, onCleanup) => handler.call(ctx, value, oldValue, onCleanup),
      options
    )
  })
}

export function useLegacyKeyboardMixin(legacyMixin) {
  const instance = getCurrentInstance()
  const proxy = instance && instance.proxy
  if (!proxy || !legacyMixin || typeof legacyMixin !== 'object') return {}

  const mixins = flattenMixins(legacyMixin)
  const mergedState = {}
  mixins.forEach((m) => {
    if (typeof m.data === 'function') {
      Object.assign(mergedState, m.data.call(proxy) || {})
    }
  })

  const state = reactive(mergedState)
  const stateRefs = toRefs(state)
  const methods = {}
  const computedRefs = {}
  const type = instance && instance.type
  const ownOverrideKeys = new Set([
    ...Object.keys((type && type.methods) || {}),
    ...Object.keys((type && type.computed) || {})
  ])
  const ctx = createLegacyContext(proxy, state, methods, computedRefs, ownOverrideKeys)

  mixins.forEach((m) => {
    const methodMap = m.methods || {}
    Object.keys(methodMap).forEach((name) => {
      const fn = methodMap[name]
      if (typeof fn !== 'function') return
      methods[name] = fn.bind(ctx)
    })
  })

  mixins.forEach((m) => {
    const computedMap = m.computed || {}
    Object.keys(computedMap).forEach((name) => {
      const entry = computedMap[name]
      if (typeof entry === 'function') {
        computedRefs[name] = computed(() => entry.call(ctx))
        return
      }
      if (entry && typeof entry === 'object' && typeof entry.get === 'function') {
        computedRefs[name] = computed({
          get: () => entry.get.call(ctx),
          set: (value) => {
            if (typeof entry.set === 'function') entry.set.call(ctx, value)
          }
        })
      }
    })
  })

  mixins.forEach((m) => {
    if (typeof m.created === 'function') {
      m.created.call(ctx)
    }
  })

  LIFECYCLE_HOOKS.forEach(([hookName, register]) => {
    mixins.forEach((m) => {
      const hook = m[hookName]
      if (typeof hook === 'function') {
        register(() => hook.call(ctx))
      }
    })
  })

  mixins.forEach((m) => {
    const watchMap = m.watch || {}
    Object.keys(watchMap).forEach((watchKey) => {
      registerWatch(watchKey, watchMap[watchKey], ctx)
    })
  })

  // 嵌套 mixin 若是「composable 桥接」（仅有 setup()），Vue3 不会执行它们的 setup，
  // 这里主动调用并合并其返回值，恢复 Vue2 时代 mixins 递归组合的效果。
  // 通过 invokeBridgeSetup 按（实例, mixin）去重，避免与全局桥接运行时重复执行。
  const nestedSetupBindings = {}
  mixins.forEach((m) => {
    if (typeof m.setup === 'function') {
      const result = invokeBridgeSetup(m)
      if (result && typeof result === 'object') {
        Object.assign(nestedSetupBindings, result)
      }
    }
  })

  return {
    ...nestedSetupBindings,
    ...stateRefs,
    ...computedRefs,
    ...methods
  }
}
