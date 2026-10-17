import { getRouters } from '@/api/menu'
import { getProject } from '@/api/system/project'
import { isProjectListPageView } from '@/utils/project-list-page'
import { applyMenuIconOverrides } from '@/utils/menu-icon'
import {
  isProjectRootMenu,
  keepLegacyProjectMenuRoute,
  rewriteProjectMenuPaths
} from '@/utils/project-module-path'
import { getCurrentProjectId } from '@/utils/store-context'
import { defineStore } from 'pinia'

const devToolsEnabled = process.env.VUE_APP_ENABLE_DEV_TOOLS === 'true'

let routerRef
let constantRoutesRef
let dynamicRoutesRef
let resetRouterFn
let LayoutRef
let ParentViewRef
let InnerLinkRef
let addRoutesFn
let prefixRouteNamesFn
let authRef

async function loadPermissionDeps() {
  if (routerRef) return
  const [routerMod, layoutMod, parentViewMod, innerLinkMod, helperMod, authMod] = await Promise.all([
    import('@/router'),
    import('@/layout/index'),
    import('@/components/ParentView'),
    import('@/layout/components/InnerLink'),
    import('@/router/helper'),
    import('@/plugins/auth')
  ])
  routerRef = routerMod.default
  constantRoutesRef = routerMod.constantRoutes
  dynamicRoutesRef = routerMod.dynamicRoutes
  resetRouterFn = routerMod.resetRouter
  LayoutRef = layoutMod.default
  ParentViewRef = parentViewMod.default
  InnerLinkRef = innerLinkMod.default
  addRoutesFn = helperMod.addRoutes
  prefixRouteNamesFn = helperMod.prefixRouteNames
  authRef = authMod.default
}

function isDevToolMenuComponent(component) {
  if (typeof component !== 'string') return false
  return component === 'tool/build/index'
    || component === 'tool/gen/index'
    || component === 'tool/swagger/index'
    || component.startsWith('tool/build/')
    || component.startsWith('tool/gen/')
    || component.startsWith('tool/swagger/')
    || component.startsWith('tool/project/browser')
    || component.startsWith('tool/project/option/browser')
}

function filterAsyncRouter(asyncRouterMap, lastRouter = false, type = false) {
  return asyncRouterMap.filter(route => {
    if (!devToolsEnabled && isDevToolMenuComponent(route.component)) {
      return false
    }
    if (type && route.children) {
      route.children = filterChildren(route.children)
    }
    if (route.component) {
      if (isProjectListPageView(route.component)) {
        route.meta = { ...(route.meta || {}), projectListPage: true }
      }
      if (route.component === 'Layout') {
        route.component = LayoutRef
      } else if (route.component === 'ParentView') {
        route.component = ParentViewRef
      } else if (route.component === 'InnerLink') {
        route.component = InnerLinkRef
      } else {
        route.component = loadView(route.component)
      }
    }
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, route, type)
    } else {
      delete route.children
      delete route.redirect
    }
    return true
  })
}

function filterChildren(childrenMap, lastRouter = false) {
  let children = []
  childrenMap.forEach(el => {
    if (el.children && el.children.length) {
      if (el.component === 'ParentView' && !lastRouter) {
        el.children.forEach(c => {
          c.path = el.path + '/' + c.path
          if (c.children && c.children.length) {
            children = children.concat(filterChildren(c.children, c))
            return
          }
          children.push(c)
        })
        return
      }
    }
    if (lastRouter) {
      el.path = lastRouter.path + '/' + el.path
      if (el.children && el.children.length) {
        children = children.concat(filterChildren(el.children, el))
        return
      }
    }
    children = children.concat(el)
  })
  return children
}

export function filterDynamicRoutes(routes) {
  const res = []
  routes.forEach(route => {
    if (route.permissions) {
      if (authRef.hasPermiOr(route.permissions)) res.push(route)
    } else if (route.roles) {
      if (authRef.hasRoleOr(route.roles)) res.push(route)
    }
  })
  return res
}

export const viewModules = import.meta.glob(['@/views/**/*.vue', '!@/views/tool/**'])

export const loadView = (view) => {
  const normalized = String(view).replace(/\.vue$/i, '')
  if (!devToolsEnabled && normalized.startsWith('tool/')) {
    return () => import('@/views/error/404.vue')
  }
  const suffix = `/views/${normalized}.vue`
  const match = Object.keys(viewModules).find(path => path.endsWith(suffix))
  if (match) {
    return viewModules[match]
  }
  return () => import('@/views/error/404.vue')
}

/** Resolve current project path keys for sidebar menu rewriting (3.1). */
function resolveCurrentProjectPathKeys() {
  const projectId = getCurrentProjectId()
  if (!projectId) return Promise.resolve(null)
  return getProject(projectId).then(res => {
    const project = res && res.data
    if (!project || !project.teamKey || !project.projectKey) return null
    return {
      teamKey: project.teamKey,
      projectKey: project.projectKey,
      projectId: project.projectId
    }
  }).catch(() => null)
}

/**
 * Remove whitelist business children from dynamic `/project` registration so
 * legacy URLs hit constant redirect routes (5.2) instead of duplicate mounts.
 * Integrations and `git-repo` list remain (keepLegacyProjectMenuRoute).
 *
 * @param {Array<{ path?: string, name?: string, children?: unknown[] }>} routes
 */
function stripNamespacedProjectMenuRoutes(routes) {
  if (!Array.isArray(routes)) return
  routes.forEach(route => {
    if (!isProjectRootMenu(route) || !Array.isArray(route.children)) return
    route.children = route.children.filter(child => keepLegacyProjectMenuRoute(child && child.path))
  })
}

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    routes: [],
    addRoutes: [],
    defaultRoutes: [],
    topbarRouters: [],
    sidebarRouters: [],
    routesRefreshing: false
  }),
  actions: {
    SetRoutesRefreshing(val) {
      this.routesRefreshing = !!val
    },
    setSidebarRouters(routes) {
      this.sidebarRouters = routes
    },
    GenerateRoutes() {
      return loadPermissionDeps().then(() => Promise.all([
        getRouters(),
        resolveCurrentProjectPathKeys()
      ]).then(([res, pathKeys]) => {
        try {
          const routeData = Array.isArray(res.data) ? res.data : []
          const sdata = JSON.parse(JSON.stringify(routeData))
          const rdata = JSON.parse(JSON.stringify(routeData))
          applyMenuIconOverrides(sdata)
          applyMenuIconOverrides(rdata)
          // Sidebar/topbar: prefix project-function menus with /{teamKey}/{projectKey}/{module}.
          // Router registration drops whitelist modules (served by namespaced constant routes);
          // integrations stay on legacy `/project/...` (see keepLegacyProjectMenuRoute).
          if (pathKeys) {
            rewriteProjectMenuPaths(sdata, pathKeys.teamKey, pathKeys.projectKey)
          }
          stripNamespacedProjectMenuRoutes(rdata)
          const sidebarRoutes = filterAsyncRouter(sdata)
          const rewriteRoutes = filterAsyncRouter(rdata, false, true)
          const asyncRoutes = filterDynamicRoutes(dynamicRoutesRef)
          prefixRouteNamesFn(rewriteRoutes)
          prefixRouteNamesFn(asyncRoutes)
          rewriteRoutes.push({ path: '/:pathMatch(.*)*', redirect: '/404', hidden: true })
          resetRouterFn()
          addRoutesFn(routerRef, rewriteRoutes)
          addRoutesFn(routerRef, asyncRoutes)
          this.addRoutes = rewriteRoutes
          this.routes = constantRoutesRef.concat(rewriteRoutes)
          this.sidebarRouters = constantRoutesRef.concat(sidebarRoutes)
          this.defaultRoutes = sidebarRoutes
          this.topbarRouters = sidebarRoutes
          return rewriteRoutes
        } catch (error) {
          console.error('[GenerateRoutes]', error)
          return Promise.reject(error)
        }
      }))
    }
  }
})
