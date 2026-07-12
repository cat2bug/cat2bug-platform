/** vue-router 4 helpers (replaces Router.prototype.addRoutes). */
export function addRoutes(router, routes) {
  routes.forEach(route => {
    router.addRoute(route)
  })
}

/**
 * Vue Router 4 全局 name 唯一：后注册的同名路由会覆盖先注册的（如 Admin/Project 覆盖 /project）。
 * 子路由 name 拼接父级 name：Project + Defect → ProjectDefect。
 */
export function prefixRouteNames(routes, parentName = '') {
  if (!Array.isArray(routes)) return
  routes.forEach(route => {
    if (route.name) {
      route.name = parentName ? `${parentName}${route.name}` : route.name
    }
    if (route.children?.length) {
      prefixRouteNames(route.children, route.name)
    }
  })
}
