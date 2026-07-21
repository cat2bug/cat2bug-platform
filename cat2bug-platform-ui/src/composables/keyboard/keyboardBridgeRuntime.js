/**
 * 键盘 mixin「桥接」运行时。
 *
 * 背景：Vue 3 不会执行「写在 mixin 里的 setup()」。项目在 Vue 3 迁移时把大量键盘增强
 * mixin（列表查询导航、列选择浮层、页面动作提示、弹框快捷键等）改成了
 * `{ setup() { return useXxx() } }` 的桥接形式，导致这些 composable 从未被执行，页面上
 * 出现「Property "xxx" was accessed during render but is not defined on instance」告警，
 * 且部分交互（如 @show 绑定到 undefined）会直接报错。
 *
 * 本模块提供一个全局 mixin：在每个组件自身的 created() 阶段，识别它声明的桥接 mixin，
 * 主动执行这些 mixin 的 setup()（此时 getCurrentInstance() 有效、可注册 onMounted 等钩子），
 * 并把返回的绑定暴露到实例上。组件自身在 methods/computed 中定义的同名成员保持优先。
 */
import { getCurrentInstance, isRef } from 'vue'

// 每个组件实例一份缓存，保证同一个桥接 mixin 在单个实例内只执行一次，
// 避免「组件直接声明 + 被其它桥接嵌套」导致的重复注册。
const instanceBridgeCache = new WeakMap()

/**
 * 执行一个桥接 mixin 的 setup()，按（实例, mixin）去重。
 * @param {object} mixin 具有 setup() 的 mixin 对象
 * @returns {object|null} setup() 的返回值
 */
export function invokeBridgeSetup(mixin) {
  if (!mixin || typeof mixin.setup !== 'function') return null
  const instance = getCurrentInstance()
  if (!instance) {
    const result = mixin.setup()
    return result && typeof result === 'object' ? result : null
  }
  let cache = instanceBridgeCache.get(instance)
  if (!cache) {
    cache = new Map()
    instanceBridgeCache.set(instance, cache)
  }
  if (cache.has(mixin)) return cache.get(mixin)
  // 先占位，避免潜在的相互嵌套导致的无限递归
  cache.set(mixin, {})
  const result = mixin.setup()
  const normalized = result && typeof result === 'object' ? result : {}
  cache.set(mixin, normalized)
  return normalized
}

/** 递归展开组件声明的 mixins（含嵌套），保持声明顺序、父在子后。 */
export function flattenComponentMixins(type, out = [], seen = new Set()) {
  if (!type || typeof type !== 'object' || seen.has(type)) return out
  seen.add(type)
  const list = Array.isArray(type.mixins) ? type.mixins : []
  list.forEach((child) => flattenComponentMixins(child, out, seen))
  list.forEach((child) => {
    if (child && typeof child === 'object') out.push(child)
  })
  return out
}

/** 把桥接 composable 返回的绑定暴露到实例上下文，供模板与其它选项访问。 */
export function exposeBridgeBindings(instance, bindings, ownKeys) {
  if (!instance || !bindings) return
  const ctx = instance.ctx
  const setupState = instance.setupState
  if (!ctx) return
  Object.keys(bindings).forEach((key) => {
    // 组件自身定义的成员优先
    if (ownKeys && ownKeys.has(key)) return
    // 不覆盖 setup() 已返回或已存在于上下文的成员
    if (setupState && Object.prototype.hasOwnProperty.call(setupState, key)) return
    if (Object.prototype.hasOwnProperty.call(ctx, key)) return
    const value = bindings[key]
    // 只挂到 ctx（setupState 结束后不可扩展；$_ 私有方法也不能经 setup() return）
    if (isRef(value)) {
      Object.defineProperty(ctx, key, {
        configurable: true,
        enumerable: true,
        get: () => value.value,
        set: (v) => { value.value = v }
      })
    } else {
      Object.defineProperty(ctx, key, {
        configurable: true,
        enumerable: true,
        get: () => value,
        set: () => {}
      })
    }
  })
}

/**
 * 若需在组件 setup() 中合并桥接结果，过滤 Vue 保留前缀（$/_），避免 setup return 告警。
 * 私有 $_ 方法仍应由 created 阶段的 exposeBridgeBindings 挂到 ctx。
 */
export function publicSetupBindings(bindings) {
  const out = {}
  if (!bindings || typeof bindings !== 'object') return out
  Object.keys(bindings).forEach((key) => {
    if (key.charAt(0) === '_' || key.charAt(0) === '$') return
    out[key] = bindings[key]
  })
  return out
}

/**
 * 生成一个全局 mixin：在组件 created() 时执行其声明的桥接 mixin 并暴露绑定。
 * @param {Set<object>} _bridgeSet 历史参数（曾用于引用匹配）；现按 mixin.setup 识别，保留签名兼容
 */
export function createKeyboardBridgeMixin(_bridgeSet) {
  return {
    created() {
      const instance = getCurrentInstance()
      if (!instance) return
      const type = instance.type
      if (!type || !Array.isArray(type.mixins)) return
      const declared = flattenComponentMixins(type)
      // Vue3 不执行 mixin.setup。对组件声明的、带 setup 的 mixin 一律桥接。
      // 不再依赖 Set 引用相等（Vite HMR 后模块身份会变，导致漏挂告警）。
      const bridges = declared.filter((mixin) => mixin && typeof mixin.setup === 'function')
      if (!bridges.length) return
      const ownKeys = new Set([
        ...Object.keys(type.methods || {}),
        ...Object.keys(type.computed || {})
      ])
      bridges.forEach((mixin) => {
        const bindings = invokeBridgeSetup(mixin)
        exposeBridgeBindings(instance, bindings, ownKeys)
      })
    }
  }
}
