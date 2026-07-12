/**
 * Vuex API shim backed by Pinia stores (Phase 1 migration bridge).
 */
import { pinia } from '@/store/pinia.js'
import { useAppStore } from '@/stores/app'
import { useDictStore } from '@/stores/dict'
import { useUserStore } from '@/stores/user'
import { useTagsViewStore } from '@/stores/tagsView'
import { usePermissionStore } from '@/stores/permission'
import { useSettingsStore } from '@/stores/settings'

function getCompatState() {
  return {
    app: useAppStore(pinia).$state,
    dict: useDictStore(pinia).$state,
    user: useUserStore(pinia).$state,
    tagsView: useTagsViewStore(pinia).$state,
    permission: usePermissionStore(pinia).$state,
    settings: useSettingsStore(pinia).$state
  }
}

const getters = {
  sidebar: () => useAppStore(pinia).sidebar,
  size: () => useAppStore(pinia).size,
  device: () => useAppStore(pinia).device,
  dict: () => useDictStore(pinia).dict,
  visitedViews: () => useTagsViewStore(pinia).visitedViews,
  cachedViews: () => useTagsViewStore(pinia).cachedViews,
  token: () => useUserStore(pinia).token,
  avatar: () => useUserStore(pinia).avatar,
  name: () => useUserStore(pinia).name,
  introduction: () => useUserStore(pinia).introduction,
  roles: () => useUserStore(pinia).roles,
  permissions: () => useUserStore(pinia).permissions,
  permission_routes: () => usePermissionStore(pinia).routes,
  topbarRouters: () => usePermissionStore(pinia).topbarRouters,
  defaultRoutes: () => usePermissionStore(pinia).defaultRoutes,
  sidebarRouters: () => usePermissionStore(pinia).sidebarRouters,
  routesRefreshing: () => usePermissionStore(pinia).routesRefreshing,
  themeMode: () => useSettingsStore(pinia).themeMode
}

const globalActions = {
  Login: (store, payload) => store.Login(payload),
  GetInfo: (store) => store.GetInfo(),
  LogOut: (store) => store.LogOut(),
  FedLogOut: (store) => store.FedLogOut(),
  UpdateCurrentProjectId: (store, payload) => store.UpdateCurrentProjectId(payload),
  SwitchCurrentProject: (store, payload) => store.SwitchCurrentProject(payload),
  GenerateRoutes: (store) => store.GenerateRoutes(),
  SetRoutesRefreshing: (store, payload) => store.SetRoutesRefreshing(payload)
}

const namespacedStores = {
  app: useAppStore,
  dict: useDictStore,
  settings: useSettingsStore,
  tagsView: useTagsViewStore
}

function resolveAction(type, name) {
  if (globalActions[name]) {
    if (['GenerateRoutes', 'SetRoutesRefreshing'].includes(name)) {
      return { store: usePermissionStore(pinia), fn: globalActions[name] }
    }
    return { store: useUserStore(pinia), fn: globalActions[name] }
  }
  if (type.includes('/')) {
    const [module, action] = type.split('/')
    const useStore = namespacedStores[module]
    if (useStore) {
      return { store: useStore(pinia), fn: (s, p) => s[action](p) }
    }
  }
  return null
}

const storeCompat = {
  get state() {
    return getCompatState()
  },
  getters,
  dispatch(type, payload) {
    const name = type.includes('/') ? type.split('/').pop() : type
    const resolved = resolveAction(type, name)
    if (!resolved) {
      return Promise.reject(new Error(`Unknown action ${type}`))
    }
    const result = resolved.fn(resolved.store, payload)
    return result instanceof Promise ? result : Promise.resolve(result)
  },
  commit(type, payload) {
    if (type === 'SET_SIDEBAR_ROUTERS') {
      usePermissionStore(pinia).setSidebarRouters(payload)
    }
    return Promise.resolve()
  }
}

export function mapState(namespace, states) {
  if (states === undefined && namespace && typeof namespace === 'object' && !Array.isArray(namespace)) {
    const res = {}
    Object.keys(namespace).forEach(key => {
      const mapper = namespace[key]
      res[key] = function mappedObjectState() {
        return typeof mapper === 'function' ? mapper(getCompatState(), this.$store.getters) : undefined
      }
    })
    return res
  }
  if (Array.isArray(namespace)) {
    states = namespace
    namespace = null
  }
  const res = {}
  states.forEach(key => {
    res[key] = function mappedState() {
      const state = getCompatState()
      if (namespace) {
        return state[namespace] ? state[namespace][key] : undefined
      }
      if (Object.prototype.hasOwnProperty.call(state, key)) {
        return state[key]
      }
      for (const mod of Object.keys(state)) {
        if (state[mod] && Object.prototype.hasOwnProperty.call(state[mod], key)) {
          return state[mod][key]
        }
      }
      return undefined
    }
  })
  return res
}

export function mapGetters(gettersMap) {
  const res = {}
  const keys = Array.isArray(gettersMap) ? gettersMap : Object.keys(gettersMap)
  keys.forEach(key => {
    res[key] = function mappedGetter() {
      const getter = getters[key]
      return typeof getter === 'function' ? getter() : getter
    }
  })
  return res
}

export function mapActions(namespace, actions) {
  if (Array.isArray(namespace)) {
    actions = namespace
    namespace = null
  }
  const res = {}
  actions.forEach(key => {
    res[key] = function mappedAction(payload) {
      const type = namespace ? `${namespace}/${key}` : key
      return storeCompat.dispatch(type, payload)
    }
  })
  return res
}

export function mapMutations(namespace, mutations) {
  if (Array.isArray(namespace)) {
    mutations = namespace
    namespace = null
  }
  const res = {}
  mutations.forEach(key => {
    res[key] = function mappedMutation(payload) {
      return storeCompat.commit(namespace ? `${namespace}/${key}` : key, payload)
    }
  })
  return res
}

export default storeCompat
