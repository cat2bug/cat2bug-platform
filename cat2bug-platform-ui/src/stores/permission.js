import { getRouters } from '@/api/menu'
import { isProjectListPageView } from '@/utils/project-list-page'
import { applyMenuIconOverrides } from '@/utils/menu-icon'
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
    || component.startsWith('tool/build/')
    || component.startsWith('tool/gen/')
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
      return loadPermissionDeps().then(() => getRouters().then(res => {
        try {
          const routeData = Array.isArray(res.data) ? res.data : []
          const sdata = JSON.parse(JSON.stringify(routeData))
          const rdata = JSON.parse(JSON.stringify(routeData))
          applyMenuIconOverrides(sdata)
          applyMenuIconOverrides(rdata)
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
