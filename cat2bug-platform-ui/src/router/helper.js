/** 记录 addRoute 返回的卸载函数，供 resetRouter 清掉无 name 的父级壳。 */
let dynamicRouteRemovers = []

/** vue-router 4 helpers (replaces Router.prototype.addRoutes). */
export function addRoutes(router, routes) {
  routes.forEach(route => {
    const remove = router.addRoute(route)
    if (typeof remove === 'function') {
      dynamicRouteRemovers.push(remove)
    }
  })
}

/** 移除本会话通过 addRoutes 注入的全部动态路由（含无名 Layout 父级）。 */
export function clearAddedRoutes() {
  const removers = dynamicRouteRemovers.splice(0, dynamicRouteRemovers.length)
  removers.forEach(remove => {
    try {
      remove()
    } catch (_e) {
      /* 路由可能已被同名覆盖移除 */
    }
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
