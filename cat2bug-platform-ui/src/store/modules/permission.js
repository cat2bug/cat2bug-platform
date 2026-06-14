import auth from '@/plugins/auth'
import router, { constantRoutes, dynamicRoutes, resetRouter } from '@/router'
import { getRouters } from '@/api/menu'
import Layout from '@/layout/index'
import { isProjectListPageView } from '@/utils/project-list-page'
import ParentView from '@/components/ParentView'
import InnerLink from '@/layout/components/InnerLink'

const devToolsEnabled = process.env.VUE_APP_ENABLE_DEV_TOOLS === 'true'

function isDevToolMenuComponent(component) {
  if (typeof component !== 'string') {
    return false
  }
  return component === 'tool/build/index'
    || component === 'tool/gen/index'
    || component.startsWith('tool/build/')
    || component.startsWith('tool/gen/')
}

const permission = {
  state: {
    routes: [],
    addRoutes: [],
    defaultRoutes: [],
    topbarRouters: [],
    sidebarRouters: [],
    /** 切换团队/项目并重拉动态路由时为 true，用于避免侧栏项目菜单多次闪烁 */
    routesRefreshing: false
  },
  mutations: {
    SET_ROUTES: (state, routes) => {
      state.addRoutes = routes
      state.routes = constantRoutes.concat(routes)
    },
    SET_DEFAULT_ROUTES: (state, routes) => {
      state.defaultRoutes = constantRoutes.concat(routes)
    },
    SET_TOPBAR_ROUTES: (state, routes) => {
      state.topbarRouters = routes
    },
    SET_SIDEBAR_ROUTERS: (state, routes) => {
      state.sidebarRouters = routes
    },
    SET_ROUTES_REFRESHING: (state, val) => {
      state.routesRefreshing = !!val
    },
  },
  actions: {
    SetRoutesRefreshing({ commit }, val) {
      commit('SET_ROUTES_REFRESHING', val)
    },
    // 生成路由
    GenerateRoutes({ commit }) {
      return new Promise((resolve, reject) => {
        getRouters().then(res => {
          try {
            const routeData = Array.isArray(res.data) ? res.data : []
            const sdata = JSON.parse(JSON.stringify(routeData))
            const rdata = JSON.parse(JSON.stringify(routeData))
            const sidebarRoutes = filterAsyncRouter(sdata)
            const rewriteRoutes = filterAsyncRouter(rdata, false, true)
            const asyncRoutes = filterDynamicRoutes(dynamicRoutes)
            rewriteRoutes.push({ path: '*', redirect: '/404', hidden: true })
            resetRouter()
            router.addRoutes(rewriteRoutes)
            router.addRoutes(asyncRoutes)
            commit('SET_ROUTES', rewriteRoutes)
            commit('SET_SIDEBAR_ROUTERS', constantRoutes.concat(sidebarRoutes))
            commit('SET_DEFAULT_ROUTES', sidebarRoutes)
            commit('SET_TOPBAR_ROUTES', sidebarRoutes)
            resolve(rewriteRoutes)
          } catch (error) {
            reject(error)
          }
        }).catch(reject)
      })
    }
  }
}

// 遍历后台传来的路由字符串，转换为组件对象
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
      // Layout ParentView 组件特殊处理
      if (route.component === 'Layout') {
        route.component = Layout
      } else if (route.component === 'ParentView') {
        route.component = ParentView
      } else if (route.component === 'InnerLink') {
        route.component = InnerLink
      } else {
        route.component = loadView(route.component)
      }
    }
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, route, type)
    } else {
      delete route['children']
      delete route['redirect']
    }
    return true
  })
}

function filterChildren(childrenMap, lastRouter = false) {
  var children = []
  childrenMap.forEach((el, index) => {
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

// 动态路由遍历，验证是否具备权限
export function filterDynamicRoutes(routes) {
  const res = []
  routes.forEach(route => {
    if (route.permissions) {
      if (auth.hasPermiOr(route.permissions)) {
        res.push(route)
      }
    } else if (route.roles) {
      if (auth.hasRoleOr(route.roles)) {
        res.push(route)
      }
    }
  })
  return res
}

const LOAD_VIEW_USE_REQUIRE = ['development', 'embedded', 'embedded-quarkus']

export const loadView = (view) => {
  if (LOAD_VIEW_USE_REQUIRE.includes(process.env.NODE_ENV)) {
    return (resolve) => require([`@/views/${view}`], resolve)
  }
  // 使用 import 实现生产环境的路由懒加载
  return () => import(`@/views/${view}`)
}

export default permission
