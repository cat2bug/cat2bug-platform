import { pinia } from './pinia.js'
import { useAppStore } from '@/stores/app'
import { useDictStore } from '@/stores/dict'
import { useUserStore } from '@/stores/user'
import { useTagsViewStore } from '@/stores/tagsView'
import { usePermissionStore } from '@/stores/permission'
import { useSettingsStore } from '@/stores/settings'

export { pinia }

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

const namespacedActions = {
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
    const useStore = namespacedActions[module]
    if (useStore) {
      return { store: useStore(pinia), fn: (s, p) => s[action](p) }
    }
  }
  return null
}

const getterDefinitions = {
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

const getters = {}
Object.keys(getterDefinitions).forEach((key) => {
  Object.defineProperty(getters, key, {
    enumerable: true,
    get: getterDefinitions[key]
  })
})

const storeCompat = {
  get state() {
    return {
      app: useAppStore(pinia).$state,
      dict: useDictStore(pinia).$state,
      user: useUserStore(pinia).$state,
      tagsView: useTagsViewStore(pinia).$state,
      permission: usePermissionStore(pinia).$state,
      settings: useSettingsStore(pinia).$state
    }
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

export default storeCompat
